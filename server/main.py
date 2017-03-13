from BaseHTTPServer import HTTPServer
from BaseHTTPServer import BaseHTTPRequestHandler
import Cookie
import zlib
import graphlab
import os
import tempfile

import models_config

# HTTPRequestHandler class
class HTTPServer_RequestHandler(BaseHTTPRequestHandler):



    def get_msg(self):
        gz_blob = self.rfile.read(int(self.headers['Content-Length']))
        message = zlib.decompress(gz_blob, 16+zlib.MAX_WBITS)
        return message


    def save_msg_to_file(self, path):
        message = self.get_msg()
        if os.path.isfile(path): #user already has data
            message = "\n".join(message.split("\n")[1:]) + "\n" #remove csv headers

        with open(path, 'a') as f:
            f.write(message)

    def update_taps(self):
        path = "data/" + self.user + "_taps.csv"
        save_msg_to_file(path)

    def update_apps(self):
        path = "data/" + self.user + "_apps.csv"
        save_msg_to_file(path)

    def train_taps(self):
        path = "data/" + self.user + "_taps.csv"
        user_taps_sf = graphlab.SFrame.read_csv()
        print user_taps_sf

        not_noise = user_taps_sf[user_taps_sf['noise'] == 0] #check
        swipes = not_noise[not_noise['type'] == "SWIPE"]
        touches = not_noise[not_noise['type'] == "TOUCH"]

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

        noise_model.save("models/" + self.user + "noise_model")
        type_model.save("models/" + self.user + "type_model")
        swipe_model.save("models/" + self.user + "swipe_model")
        touch_model.save("models/" + self.user + "touch_model")




    def predict_taps(self):
        path = "/tmp/" + self.user + "_temp_taps.csv"
        save_msg_to_file(path)
        taps_sf = graphlab.SFrame.read_csv(path)
        noise_model = graphlab.load_model("models/" + self.user + "noise_model")
        type_model = graphlab.load_model("models/" + self.user + "type_model")
        touch_model = graphlab.load_model("models/" + self.user + "touch_model")
        swipe_model = graphlab.load_model("models/" + self.user + "swipe_model")

        result = []

        for tap_sf in taps_sf:
            if noise_model.predict(tap_sf):
                result.append("NOISE")
                continue
            if type_model.predict(tap_sf) == "SWIPE":
                result.append(swipe_model.predict(tap_sf))
            else:
                result.append(touch_model.predict(tap_sf))

        print result

    def do_POST(self):

        c = Cookie.SimpleCookie()
        try:
            c.load(self.headers.get("Cookie"))
            self.user = c['user'].value
            if not self.user.isdigit():
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


def run():
  print('starting server...')

  server_address = ('0.0.0.0', 8081)
  httpd = HTTPServer(server_address, HTTPServer_RequestHandler)
  print('running server...')
  httpd.serve_forever()

if __name__ == "__main__":
    run()
