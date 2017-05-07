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

from collections import Counter
from StringIO import StringIO
import pandas as pd

import models_config


models = {}

class AppPredictor():

    def __init__(self):
        self.user = user
        self.queue = Queue.Queue()
        self.apps_sfs = self.load_apps_sfs()


    def _loop(self):
        received = []
        while(True):
            word = self.queue.get(block=True)
            if word == "BYE":
                print "And the last one leaves"
                return
            # and magic happens here


    def load_apps_sf(self):
        path = "data/" + self.user + "_apps.csv"
        apps_sf = graphlab.SFrame.read_csv(path)
        for app in apps_sf['app'].unique():
            this_app_sf = apps_sf[apps_sf['app'] == app]
            # do stuff here so we get bag of words and so on

    def start(self):
        t = threading.Thread(target = self._loop)
        t.start()
        return t



class Compressor():

    MAX_CONSECUTIVE_NOISE = 10
    MAX_CONSECUTIVE_NOT_NOISE = 10
    MAX_BLOCK_SIZE = 30

    def __init__(self, user, mode):
        self.user = user
        self.mode = mode
        self.queue = Queue.Queue()
        self.lock = threading.Lock() # Maybe wont use

    def get_more_commons(self, block):
        l = len(block)
        if l > Compressor.MAX_BLOCK_SIZE:
            left = self.get_more_commons(block[:l/2])
            right = self.get_more_commons(block[l/2:])
            return left + right
        data = Counter(block)
        return [data.most_common(1)[0][0]]

    def pass_compressed(self, compressed):
        if mode == "PREDICT TAPS":
            print "\n".join(compressed)
        elif mode == "UPDATE APPS":
            path = "data/" + self.user + "_apps.csv"
            if not os.path.isfile(path): #user doesn't have data
                with open(path, "w") as f:
                    f.write("word, app\n")
            with open(path, "a") as f:
                for word in compressed:
                    f.write(word + "," + self.app + "\n")
        elif mode == "PREDICT APPS":
            for word in compressed:
                self.app_predictor.queue.put_nowait(word)

        
    
    def _loop(self):
        n_noise = 0
        n_not_noise = 0
        in_noise_block = True
        current_block = []
        result = []
        while(True):
            sf = self.queue.get(block=True)
            if type(sf) == str:
                print "otro que se va"
                if self.app_predictor:
                    self.app_predictor.queue.put_nowait("BYE")
                return
            print sf["prediction"], n_noise, n_not_noise, in_noise_block, current_block, result
            if sf["prediction"][0] == "NOISE": # if we get noise
                if in_noise_block: # and we are in a noise block
                    n_not_noise = 0 # reset not_noise counter
                    current_block = [] # reset block just in case
                    continue # and keep reading
                n_noise += 1 #otherwise we are in an event block, add to noise_counter
                if n_noise >= Compressor.MAX_CONSECUTIVE_NOISE: # if we have enough consecutive noise
                    compressed = self.get_more_commons(current_block) # get compressed
                    result.extend(compressed) # add to compressed result
                    self.pass_compressed(compressed) # pass compressed
                    in_noise_block = True # go back to being in a noise block
                    n_noise = n_not_noise = 0 # reset counters
                    current_block = [] # and reset current block
            else: # if we get something that is not noise
                current_block.append(sf["prediction"][0]) # add to posibly current block
                if in_noise_block: # if we are in a block of noise
                    n_not_noise += 1 # add to not_noise_counter
                    if n_not_noise >= Compressor.MAX_CONSECUTIVE_NOT_NOISE: # if we have enough consecutive not noise
                        in_noise_block = False # leave noise block state
                        n_noise = n_not_noise = 0 # reset counter
                else: # if we are not in a noise block
                    n_noise = 0 #reset noise counter                    

    def start(self):
        t = threading.Thread(target = self._loop)
        t.start()
        if mode == "PREDICT_APPS":
            self.app_predictor = AppPredictor(self.user)
            self.app_predictor_t = self.app_predictor.start()
        return t
        

class TapPredictor():

    def __init__(self, user, mode):
        self.user = user
        self.queue = Queue.Queue()
        self.noise_model = models[self.user]['noise']
        self.type_model = models[self.user]['type'] 
        self.touch_model = models[self.user]['touch']
        self.swipe_model = models[self.user]['swipe']
        self.mode = mode
        self.lock = threading.Lock()
        self.lock.acquire()

    def get_prediction(self):
        self.lock.acquire()
        self.lock.release()

    def set_app(self, app):
        self.app = app
        compressor.app = app

    def _loop(self):
        while(True):
            sf = self.queue.get(block=True)
            if type(sf) == str:
                print "si ya saben como me pongo pa que me invitan"
                self.compressor.queue.put_nowait(sf) # sf is actually a string
                return
            if self.noise_model.predict(sf)[0]: #noise
                sf["prediction"] = "NOISE"
            else:
                e_type = self.type_model.predict(sf)[0]
                if e_type == "SWIPE":
                    sf["prediction"] = self.swipe_model.predict(sf)[0]
                else:
                    sf["prediction"] = self.touch_model.predict(sf)[0]
            self.compressor.queue.put_nowait(sf)
            
        print "nigga bye"

    def start(self):
         t = threading.Thread(target=self._loop)
         t.start()
         self.compressor = Compressor(self.user, self.mode)
         self.compressor_thread = self.compressor.start()
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
        predictor = TapPredictor(self.user, "UPDATE APPS")
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
