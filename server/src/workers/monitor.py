import time
import threading
from Queue import Queue

class Monitor():

    loop = True

    def __init__(self, tap_predictor):

        self.tap_predictor_queue = tap_predictor.queue
        self.compressor_queue = tap_predictor.compressor.queue
        """if tap_predictor.mode == "PREDICT_APPS":
            self.app_predictor_queue = tap_predictor.compressor.app_predictor.queue"""


    def _loop(self):
        print("MONITORING!")
        change = True
        workers_before, compressor_before = (-1, -1)
        while self.loop:
            workers = self.tap_predictor_queue.qsize()
            compressor = self.compressor_queue.qsize()
            print("Workers: {}, compressor: {}".format(workers, compressor))

            if workers_before == workers and compressor_before == compressor:
                break
            workers_before, compressor_before = workers, compressor
            time.sleep(2)

        self.loop = True
        print("Monitor leaving")


    def stop(self):
        self.loop = False
        return

    def start(self):
        self.my_thread = threading.Thread(target=self._loop)
        self.my_thread.daemon = True
        self.my_thread.start()
