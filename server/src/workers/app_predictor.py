from Queue import Queue
import threading

import graphlab


class AppPredictor():

    def __init__(self):
        self.user = user
        self.queue = Queue(0)
        self.apps_sfs = self.load_apps_sfs()


    def _loop(self):
        received = []
        while(True):
            word = self.queue.get(block=True)
            if word == "BYE":
                sys.stderr.write("And the last one leaves")
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
