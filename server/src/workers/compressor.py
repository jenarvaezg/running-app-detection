from Queue import PriorityQueue as Queue
import threading
import random
import os

from time import time

from collections import Counter

from workers.app_predictor import AppPredictor


class Compressor():

    MAX_CONSECUTIVE_NOISE = 15
    MAX_CONSECUTIVE_NOT_NOISE = 25
    MAX_BLOCK_SIZE = 60

    def __init__(self, user, mode):
        self.session_id = ''.join(random.choice('0123456789ABCDEF') for i in range(16))
        self.user = user
        self.mode = mode
        self.queue = Queue(0)

    def get_most_commons(self, block):
        l = len(block)

        if l > Compressor.MAX_BLOCK_SIZE:
            left = self.get_most_commons(block[:l/2])
            right = self.get_most_commons(block[l/2:])
            return tuple(left[i] + right[i] for i in range(len(left)))

	word_weights = {}
	for e in block:
		word_weights[e['word']] = word_weights.setdefault(e['word'], 0) + (1/e['probability_noise'])
	
        words = [e['word'] for e in block]
        data = Counter(words)


	print word_weights, data
	most_common_word = max(word_weights, key=word_weights.get)

        return ([most_common_word], [block[l/2]["time"]], [block[l/2]['probability_noise']])

    def pass_compressed(self, compressed, times):
        print("HEY I GOT", compressed, times)
        if self.mode == "PREDICT_TAPS":
            print "\n".join(compressed)
        elif self.mode == "UPDATE_APPS":
            path = "data/" + self.user + "_apps.csv"
            if not os.path.isfile(path): #user doesn't have data
                with open(path, "w") as f:
                    f.write("word,timestamp,app,session_id\n")
            with open(path, "a") as f:
                for i in range(len(compressed)):
                    f.write(compressed[i] + "," + str(times[i]) + "," + self.app + "," + self.session_id + "\n")
        elif self.mode == "PREDICT_APPS":
            for i in range(len(compressed)):
                self.app_predictor.queue.put({'word': compressed[i], 'timestamp': times[i]})

    def _loop(self):
        n_noise = 0
        n_not_noise = 0
        in_noise_block = True
        current_block = []

        while(True):
            sf = self.queue.get(block=True)[1]
            if type(sf) == str:
                self._exit_operations()
                return

            if sf["prediction"][0] == "NOISE": # if we get noise
                if in_noise_block: # and we are in a noise block
                    n_not_noise = 0 # reset not_noise counter
                    current_block = [] # reset block just in case
                    continue # and keep reading
                n_noise += 1 #otherwise we are in an event block, add to noise_counter
                if n_noise >= Compressor.MAX_CONSECUTIVE_NOISE: # if we have enough consecutive noise
                    compressed, times, probability_noise = self.get_most_commons(current_block) # get compressed
                    self.pass_compressed(compressed, times) # pass compressed
                    in_noise_block = True # go back to being in a noise block
                    n_noise = n_not_noise = 0 # reset counters
                    current_block = [] # and reset current block
            else: # if we get something that is not noise
                current_block.append({'word': sf["prediction"][0], 'probability_noise': sf['probability_noise'][0], 'time': int(sf["timestamp"][0])}) # add to posibly current block
                if in_noise_block: # if we are in a block of noise
                    n_not_noise += 1 # add to not_noise_counter
                    if n_not_noise >= Compressor.MAX_CONSECUTIVE_NOT_NOISE: # if we have enough consecutive not noise
                        in_noise_block = False # leave noise block state
                        n_noise = n_not_noise = 0 # reset counter
                else: # if we are not in a noise block
                    n_noise = 0 #reset noise counter

    def _exit_operations(self):
        if self.mode == "PREDICT_APPS":
            self.app_predictor.queue.put("BYE")
            print("Compressor waiting for app predictor")
            self.app_predictor_t.join()
        elif self.mode == "UPDATE_APPS":
            print "Generating app model for user", self.user
            AppPredictor.generate_app_model(self.user)
            print "Model saved"

        print "Compressor leaving, mode was", self.mode

    def start(self):
        t = threading.Thread(target = self._loop)
        t.daemon = True
        t.start()
        if self.mode == "PREDICT_APPS":
            self.app_predictor = AppPredictor(self.user, self.session_id)
            self.app_predictor_t = self.app_predictor.start()
        return t
