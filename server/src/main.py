from BaseHTTPServer import HTTPServer
from BaseHTTPServer import BaseHTTPRequestHandler
from SocketServer import ThreadingMixIn
import Cookie
import zlib
import graphlab
import os
import tempfile
import random
import threading
import Queue
import sys

from StringIO import StringIO
import pandas as pd

import config.models_config

from workers.tap_predictor import TapPredictor


models = {}


# RequestHandler class
class RequestHandler(BaseHTTPRequestHandler):


    taps_file_mutex = threading.Lock()
    apps_file_mutex = threading.Lock()

    def get_msg(self, compressed=False):
        blob = self.rfile.read(int(self.headers['Content-Length']))
	if not compressed:
		return blob
        message = zlib.decompress(gz_blob, 16+zlib.MAX_WBITS)
        return message


    def save_msg_to_file(self, path, compressed=False):
        message = self.get_msg(compressed)
        if os.path.isfile(path): #user already has data
            message = "\n".join(message.split("\n")[1:]) + "\n" #remove csv headers

        with open(path, 'a') as f:
            RequestHandler.taps_file_mutex.acquire()
            f.write(message)
            RequestHandler.taps_file_mutex.release()

    def update_taps(self):
        path = "data/" + self.user + "_taps.csv"
        self.save_msg_to_file(path)

    def update_apps(self):
        predictor = TapPredictor(self.user, "UPDATE APPS", models[self.user])
        predictor_thread = predictor.start()

        names = self.rfile.readline().rstrip().split(",")
        for line in self.rfile:
            splitted = [[float(x)] for x in line.rstrip().split(",")] # god damnit graphlab
            d = dict(zip(names, splitted))
            sf = graphlab.SFrame(d)
            predictor.queue.put_nowait(sf)
            predictor.queue.put_nowait("BYE")
            predictor_thread.join()
        return

    def train_taps(self):
        path = "data/" + self.user + "_taps.csv"
        user_taps_sf = graphlab.SFrame.read_csv(path)
        sys.stderr.write(str(user_taps_sf) + "\n")
        sys.stderr.write(str(len(user_taps_sf)) + " TOTAL\n")
        not_noise = user_taps_sf[user_taps_sf['noise'] == 0]
        sys.stderr.write(str(len(not_noise)) + " NOT_NOISE\n")
        swipes = not_noise[not_noise['type'] == "SWIPE"]
        sys.stderr.write(str(len(swipes)) + " SWIPES\n")
        touches = not_noise[not_noise['type'] == "TOUCH"]
        sys.stderr.write(str(len(touches)) + " TOUCHES\n")
        noise_model = graphlab.boosted_trees_classifier.create(user_taps_sf,
                                                            target="noise",
                                                            features=models_config.features,
                                                            max_iterations = models_config.noise_max_iterations,
                                                            max_depth = models_config.noise_max_depth)
        type_model = graphlab.boosted_trees_classifier.create(not_noise,
                                                              target="type",
                                                              features=models_config.features,
                                                              max_iterations = models_config.type_max_iterations,
                                                              max_depth = models_config.type_max_depth)
        swipe_model = graphlab.boosted_trees_classifier.create(swipes,
                                                             target="action",
                                                             features=models_config.features,
                                                             max_iterations = models_config.swipe_max_iterations,
                                                             max_depth = models_config.swipe_max_depth)
        touch_model = graphlab.boosted_trees_classifier.create(touches,
                                                             target="action",
                                                             features=models_config.features,
                                                             max_iterations = models_config.touch_max_iterations,
                                                             max_depth = models_config.touch_max_depth)

        noise_model.save("models/" + self.user + "_noise_model")
        type_model.save("models/" + self.user + "_type_model")
        swipe_model.save("models/" + self.user + "_swipe_model")
        touch_model.save("models/" + self.user + "_touch_model")
    	load_models()

    def train_apps(self):
        return
        #doesn't work yet
        # all this might change, I might want to use the same code as the predictor up there
        compressed = compress(user_taps_sf[user_taps_sf['app' == app]])
        word_count = graphlab.text_analytics.count_words(compressed)


        #corups_words = graphlab.SFrame([i
        #compressed_sf = compress(user_taps_sf) #this should be and sf with only words and app
        # somewhere, pass it to bag of words
        #comppressed_sf['tfidf'] = graphlab


    def predict_taps(self):
        predictor = TapPredictor(self.user, "PREDICT TAPS")
        predictor_thread = predictor.start()

        names = self.rfile.readline().rstrip().split(",")
        for line in self.rfile:
            splitted = [[float(x)] for x in line.rstrip().split(",")] # god damnit graphlab
            d = dict(zip(names, splitted))
            sf = graphlab.SFrame(d)
            predictor.queue.put_nowait(sf)
        predictor.queue.put_nowait("BYE")
        predictor_thread.join()
        return

    def do_POST(self):
        c = Cookie.SimpleCookie()
        try:
            c.load(self.headers.get("Cookie"))
            self.user = c['user'].value
            if not self.user.isalnum():
                raise Exception("Non numeric user")
        except:
            self.send_response(403)
            self.end_headers()
            return

        sys.stderr.write(self.path + "\n")
        if self.path == "/train_taps":
            self.train_taps()
        elif self.path == "/train_apps":
            self.train_apps()
        elif self.path == "/update_taps":
            self.update_taps()
        elif self.path == "/update_apps":
            self.update_apps()
        elif self.path == "/predict_taps":
            self.predict_taps()
        elif self.path == "/predict_apps":
            self.predict_apps()
        else:
            self.send_response(404)
            self.end_headers()
            return

        self.send_response(200)
        self.send_header('Content-type','text/html')
        self.end_headers()
        return


class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
	"""Handle requests in a separate thread."""



def load_models():
    import os
    global models

    try:
        all_models = os.listdir("models")
    except OSError as e:
        sys.stderr.write(str(e) + "\n")
    else:
    	for model_path in all_models:
    		model = graphlab.load_model("models/" + model_path)
    		spl = model_path.split("_")
    		user = spl[0]
    		type = spl[1]
    		if not user in models:
    			models[user] = {}
    		models[user][type] = model



def run():
  sys.stderr.write('starting server...\n')

  server_address = ('0.0.0.0', 8081)
  server = ThreadedHTTPServer(server_address, RequestHandler)
  sys.stderr.write('running server...\n"')
  load_models()

  server.serve_forever()

if __name__ == "__main__":
    run()
