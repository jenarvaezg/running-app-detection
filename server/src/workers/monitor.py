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
        while self.loop:
            print("Workers: {}, compressor: {}".format(
                self.tap_predictor_queue.qsize(),
                self.compressor_queue.qsize()))
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
