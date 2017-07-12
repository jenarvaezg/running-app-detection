from Queue import Queue
import threading

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
        self.lock = threading.Lock()
        self.lock.acquire()
        self.models = models

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
                sys.stderr.write("si ya saben como me pongo pa que me invitan")
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

        sys.stderr.write("nigga bye")

    def start(self):
         t = threading.Thread(target=self._loop)
         t.start()
         self.compressor = Compressor(self.user, self.mode)
         self.compressor_thread = self.compressor.start()
         return t
