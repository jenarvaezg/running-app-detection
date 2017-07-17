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
                sys.stderr.write("And the last one leaves\n")
                return
            # and magic happens here


    def load_apps_sf(self):
        path = "data/" + self.user + "_apps.csv"
        apps_sf = graphlab.SFrame.read_csv(path)
        for app in apps_sf['app'].unique():
            this_app_sf = apps_sf[apps_sf['app'] == app]
            # do stuff here so we get bag of words and so on

            #doesn't work yet
            # all this might change, I might want to use the same code as the predictor up there
            compressed = compress(user_taps_sf[user_taps_sf['app' == app]])
            word_count = graphlab.text_analytics.count_words(compressed)


            #corups_words = graphlab.SFrame([i
            #compressed_sf = compress(user_taps_sf) #this should be and sf with only words and app
            # somewhere, pass it to bag of words
            #comppressed_sf['tfidf'] = graphlab

    def start(self):
        t = threading.Thread(target = self._loop)
        t.daemon = True
        t.start()
        return t
