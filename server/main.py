from BaseHTTPServer import HTTPServer
from BaseHTTPServer import BaseHTTPRequestHandler
from SocketServer import ThreadingMixIn
import Cookie
import zlib
import graphlab
import os
import tempfile
import random

from StringIO import StringIO
import pandas as pd

import models_config
import numpy as np

graphlab.SFrame() #so frapglab really starts now


models = {}

# RequestHandler class
class RequestHandler(BaseHTTPRequestHandler):



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
            f.write(message)

    def update_taps(self):
        path = "data/" + self.user + "_taps.csv"
        self.save_msg_to_file(path)

    def update_apps(self):
        path = "data/" + self.user + "_apps.csv"
        self.save_msg_to_file(path)

    def train_taps(self):
        path = "data/" + self.user + "_taps.csv"
        user_taps_sf = graphlab.SFrame.read_csv(path)
        print user_taps_sf

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



    def predict_taps(self):
	print "GOOOO"
	df = pd.read_csv(self.rfile)
	taps_sf = graphlab.SFrame(df)
        noise_model = models[self.user]['noise'] #graphlab.load_model("models/" + self.user + "_noise_model")
        type_model = models[self.user]['type'] #graphlab.load_model("models/" + self.user + "_type_model")
        touch_model = models[self.user]['touch'] #graphlab.load_model("models/" + self.user + "_touch_model")
        swipe_model = models[self.user]['swipe'] #graphlab.load_model("models/" + self.user + "_swipe_model")
	print "models loaded"
        result = []

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
	

	
        print result

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
	    self.send_response(200)
	    self.send_header('Content-type', 'text/html')
	    self.end_headers()
            self.predict_taps()
	    return
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
  #httpd = HTTPServer(server_address, HTTPServer_RequestHandler)
  server = ThreadedHTTPServer(server_address, RequestHandler)
  print('running server...')
  load_models()

  server.serve_forever()

if __name__ == "__main__":
    run()
