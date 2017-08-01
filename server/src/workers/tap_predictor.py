from Queue import Queue
import threading
import sys

from workers.compressor import Compressor

class TapPredictor():

    def __init__(self, user, mode, models):
        self.user = user
        self.queue = Queue(0)
        self.noise_model = models['noise']
        self.type_model = models['type']
        self.touch_model = models['touch']
        self.swipe_model = models['swipe']
        self.mode = mode
        self.workers = []
        self.NWORKERS = 2
        self.app = ""

    def set_app(self, app):
        self.app = app
        self.compressor.app = app

    def _worker_loop(self, id):
        while(True):
            sf = self.queue.get(block=True)
            if type(sf) == str:
                print("Tap Worker {} leaving".format(id))
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


    def stop(self):
        for worker in self.workers:
            self.queue.put("BYE")
            worker.join()
            print("Joined with tap worker")
        self.compressor_thread.join()
        print("Joined with compressor thread, tap predictor leaving")
        return
        

    def start(self):
        for i in range(self.NWORKERS):
            t = threading.Thread(target=self._worker_loop, args=(i, ))
            t.daemon = True
            self.workers.append(t)
            t.start()
        self.compressor = Compressor(self.user, self.mode)
        self.compressor_thread = self.compressor.start()
        return self
