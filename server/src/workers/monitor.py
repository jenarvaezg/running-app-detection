import time
import threading
from Queue import Queue

class Monitor():

    LEAVE_THRESHOLD = 4
    looping = []

    def __init__(self, tap_predictor):

        self.tap_predictor_queue = tap_predictor.queue
        self.compressor_queue = tap_predictor.compressor.queue
        self.id = tap_predictor.compressor.session_id
        Monitor.looping.append(self.id)

    def _loop(self):
        print("MONITORING!")
        n_same = 0

        workers_before, compressor_before = (-1, -1)
        while Monitor._should_loop(self.id):
            workers = self.tap_predictor_queue.qsize()
            compressor = self.compressor_queue.qsize()
            print("ID: {}, Workers: {}, compressor: {}".format(self.id, workers, compressor))
            time.sleep(2)

        print("Monitor leaving")



    def stop(self):
        Monitor.looping.remove(self.id)

    def start(self):
        self.my_thread = threading.Thread(target=self._loop)
        self.my_thread.daemon = True
        self.my_thread.start()

    @staticmethod
    def _should_loop(monitor_id):
        return monitor_id in Monitor.looping
