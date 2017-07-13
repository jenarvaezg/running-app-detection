from Queue import Queue
import threading
import sys

from workers.compressor import Compressor

class TapPredictor():

    _user_seqs = {}
    _global_lock = threading.Lock()


    @classmethod
    def init_user(cls, user):
        cls._global_lock.acquire()
        cls._user_seqs[user] = {'lock': threading.Lock(), 'seq': 0}
        cls._global_lock.release()

    @classmethod
    def _get_seq_id(cls, user):
        cls._global_lock.acquire()
        user_seqs = cls._user_seqs[user]
        cls._global_lock.release()
        user_seqs['lock'].acquire()
        seq = user_seqs['seq']
        user_seqs['seq'] += 1
        user_seqs['lock'].release()
        return seq

    def __init__(self, user, mode, models):
        self.user = user
        self.queue = Queue(0)
        self.noise_model = models['noise']
        self.type_model = models['type']
        self.touch_model = models['touch']
        self.swipe_model = models['swipe']
        self.mode = mode
        self.lock = threading.Lock()
        self.models = models
        # self.compressor_thread = compressor_thread


    def set_app(self, app):
        self.app = app
        self.compressor.app = app

    def _loop(self):
        while(True):
            sf = self.queue.get(block=True)
            if type(sf) == str:
                sys.stderr.write("si ya saben como me pongo pa que me invitan\n")
                self.compressor.queue.put_nowait(sf) # sf is actually a string
                self.compressor_thread.join()
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

        sys.stderr.write("nigga bye\n")

    def start(self):
         t = threading.Thread(target=self._loop)
         t.start()
         self.compressor = Compressor(self.user, self.mode)
         self.compressor_thread = self.compressor.start()
         return t
