from Queue import Queue
import threading
import sys

from time import time

from collections import Counter

from workers.app_predictor import AppPredictor


class Compressor():

    MAX_CONSECUTIVE_NOISE = 10
    MAX_CONSECUTIVE_NOT_NOISE = 10
    MAX_BLOCK_SIZE = 30

    def __init__(self, user, mode):
        self.user = user
        self.mode = mode
        self.queue = Queue(0)
        self.lock = threading.Lock() # Maybe wont use

    def get_most_commons(self, block):
        l = len(block)

        if l > Compressor.MAX_BLOCK_SIZE:
            left, ltime = self.get_most_commons(block[:l/2])
            right, rtime = self.get_most_commons(block[l/2:])
            return (left + right, ltime + rtime)
        data = Counter(block)
        return ([data.most_common(1)[0][0]], block[l/2]["timestamp"])

    def pass_compressed(self, compressed, times):
        if self.mode == "PREDICT TAPS":
            sys("\n".join(compressed))
        elif self.mode == "UPDATE APPS":
            path = "data/" + self.user + "_apps.csv"
            if not os.path.isfile(path): #user doesn't have data
                with open(path, "w") as f:
                    f.write("word,timestamp,app\n")
            with open(path, "a") as f:
                for i in range(len(compressed)):
                    f.write(compressed[i] + "," times[i], "," + self.app + "\n")
        elif self.mode == "PREDICT APPS":
            for word in compressed:
                self.app_predictor.queue.put(word)

    def _loop(self):
        n_noise = 0
        n_not_noise = 0
        in_noise_block = True
        current_block = []
        result = []
        while(True):
            sf = self.queue.get(block=True)
            if type(sf) == str:
                sys.stderr.write("otro que se va\n")
                if self.app_predictor:
                    self.app_predictor.queue.put("BYE")
                return
            sys.stderr.write(sf["prediction"], n_noise, n_not_noise, in_noise_block, current_block, result, "\n")
            sf['timestamp'] = time()

            if sf["prediction"][0] == "NOISE": # if we get noise
                if in_noise_block: # and we are in a noise block
                    n_not_noise = 0 # reset not_noise counter
                    current_block = [] # reset block just in case
                    continue # and keep reading
                n_noise += 1 #otherwise we are in an event block, add to noise_counter
                if n_noise >= Compressor.MAX_CONSECUTIVE_NOISE: # if we have enough consecutive noise
                    compressed, times = self.get_most_commons(current_block) # get compressed
                    result.extend(compressed) # add to compressed result
                    self.pass_compressed(compressed, times) # pass compressed
                    in_noise_block = True # go back to being in a noise block
                    n_noise = n_not_noise = 0 # reset counters
                    current_block = [] # and reset current block
            else: # if we get something that is not noise
                current_block.append(sf["prediction"][0]) # add to posibly current block
                if in_noise_block: # if we are in a block of noise
                    n_not_noise += 1 # add to not_noise_counter
                    if n_not_noise >= Compressor.MAX_CONSECUTIVE_NOT_NOISE: # if we have enough consecutive not noise
                        in_noise_block = False # leave noise block state
                        n_noise = n_not_noise = 0 # reset counter
                else: # if we are not in a noise block
                    n_noise = 0 #reset noise counter

    def start(self):
        t = threading.Thread(target = self._loop)
        t.start()
        if mode == "PREDICT_APPS":
            self.app_predictor = AppPredictor(self.user)
            self.app_predictor_t = self.app_predictor.start()
        return t
