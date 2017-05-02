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
import collections
import Queue

from StringIO import StringIO
import pandas as pd

import models_config
import numpy as np


models = {}

class Predictor():

    def __init__(self, user):
        self.user = user
        self.queue = Queue.Queue()
        self.noise_model = models[self.user]['noise']
        self.type_model = models[self.user]['type'] 
        self.touch_model = models[self.user]['touch']
        self.swipe_model = models[self.user]['swipe']
        self.lock = threading.Lock()
        self.lock.acquire()

    def get_prediction(self):
        self.lock.acquire()
        self.lock.release()


    def _loop(self):
        while(True):
            sf = self.queue.get(block=True)
            if type(sf) == str:
                print "si ya saben como me pongo pa que me invitan"
                return

        # Look at all this dead code holy shit
        """self.lock.release()


    	noise_predictions = noise_model.predict(taps_sf)
        if (noise_predictions == 1).all():
		    print "ALL NOISE"
        return
		
        noise_before = False
        for i in range(len(taps_sf)): 
            if noise_predictions[i]:
	            if not noise_before:
	                result.append("NOISE")
    			noise_before = True
	    	    continue
            noise_before = False
            if type_model.predict(taps_sf[i])[0] == "SWIPE":
                result.append(swipe_model.predict(taps_sf[i])[0])
            else:
                result.append(touch_model.predict(taps_sf[i])[0])

    	if result[-1] != "NOISE":
	        result.append("NOISE")
    	words = []
        noise_positions = [i for i, x in enumerate(result) if x == "NOISE"]
        print noise_positions	
        for i in range(len(noise_positions) -1):
            start, end = noise_positions[i] + 1, noise_positions[i+1]
            detections = result[start:end]
            counter = collections.Counter(detections)
            print counter.most_common()
            words.append(counter.most_common()[0][0])
	    

	    print words
        print result""" 


        print "nigga bye"

    def start(self):
         t = threading.Thread(target=self._loop)
         t.start()
         return t


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
        path = "data/" + self.user + "_apps.csv"
        with open(path, 'a') as f:
            RequestHandler.apps_file_mutex.acquire()
            for line in self.rfile:
                print line
                f.write(line)
            RequestHandler.apps_file_mutex.release()

    def train_taps(self):
        path = "data/" + self.user + "_taps.csv"
        user_taps_sf = graphlab.SFrame.read_csv(path)
        print user_taps_sf
        print len(user_taps_sf), "TOTAL"
        not_noise = user_taps_sf[user_taps_sf['noise'] == 0]
        print len(not_noise), "NOT_NOISE"
        swipes = not_noise[not_noise['type'] == "SWIPE"]
        print len(swipes), "SWIPES"
        touches = not_noise[not_noise['type'] == "TOUCH"]
        print len(touches), "TOUCHES"
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
        # all this might change, I might want to use the same code as the predictor up there 
        path = "data/" + self.user + "_taps.csv"
        user_taps_sf = graphlab.SFrame.read_csv(path)
    
        noise_model = models[self.user]['noise']
        type_model = models[self.user]['type']
        touch_model = models[self.user]['touch']
        swipe_model = models[self.user]['swipe']

        for sf in user_taps_sf:
            if noise_model.predict(sf) == 1:
                sf['word'] = "NOISE"
                continue
            if type_model.predict(sf) == "SWIPE":
                sf['word'] = swipe_model.predict(sf)
            else:
                sf['word'] = touch_model.predict(sf)
        return

        for app in sf['app'].unique():
            compressed = compress(user_taps_sf[user_taps_sf['app' == app]])
            word_count = graphlab.text_analytics.count_words(compressed)
            

        corups_words = graphlab.SFrame([
        compressed_sf = compress(user_taps_sf) #this should be and sf with only words and app
        # somewhere, pass it to bag of words
        comppressed_sf['tfidf'] = graphlab




    def predict_taps(self):
        predictor = Predictor(self.user)

        predictor_thread = predictor.start()

        names = self.rfile.readline().rstrip().split(",")
        for line in self.rfile:
            splitted = [[float(x)] for x in line.rstrip().split(",")] # god damnit graphlab
            d = dict(zip(names, splitted))
            sf = graphlab.SFrame(d)
            predictor.queue.put_nowait(sf)
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

        if self.path == "/train_taps":
            self.train_taps()
        elif self.path == "/train_apps":
            self.train_apps()
        if self.path == "/update_taps":
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
	all_models = os.listdir("models")
	for model_path in all_models:
		model = graphlab.load_model("models/" + model_path)
		spl = model_path.split("_")
		user = spl[0]
		type = spl[1]
		if not user in models:
			models[user] = {}
		models[user][type] = model
	
			

def run():
  print('starting server...')

  server_address = ('0.0.0.0', 8081)
  server = ThreadedHTTPServer(server_address, RequestHandler)
  print('running server...')
  load_models()

  server.serve_forever()

if __name__ == "__main__":
    run()
