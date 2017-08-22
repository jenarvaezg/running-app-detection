from Queue import Queue
import threading
import sys
import graphlab

import config.models_config as models_config
from workers.compressor import Compressor

class TapPredictor():

    def __init__(self, user, mode):
        self.user = user
        self.queue = Queue(0)
        models = self._get_models(user)
        self.noise_model = models['noise']
        self.type_model = models['type']
        self.touch_model = models['touch']
        self.swipe_model = models['swipe']
        self.mode = mode
        self.workers = []
        self.NWORKERS = 4
        self.app = ""
        self.noise_prediction_threshold = 0.5


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

            probability_noise = self.noise_model.predict(sf, output_type="probability")[0]
            if probability_noise > self.noise_prediction_threshold: #noise
                sf["prediction"] = "NOISE"
            else:
                probability_touch = self.type_model.predict(sf, output_type="probability")[0]
                if probability_touch < 0.5:
                    sf["prediction"] = self.swipe_model.predict(sf)[0]
                else:
                    sf["prediction"] = self.touch_model.predict(sf)[0]

		    type_certainty = probability_touch if probability_touch > 0.5 else 1 - probability_touch
            sf['certainty'] = [(1-probability_noise) + type_certainty]

            self.compressor.queue.put_nowait((sf['seq_n'][0], sf))


    def stop(self):
        for i in range(len(self.workers)):
            self.queue.put("BYE")
        for worker in self.workers:
            worker.join()
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

    @classmethod
    def generate_tap_model(cls, user):
        path = "data/" + user + "_taps.csv"
        user_taps_sf = graphlab.SFrame.read_csv(path, verbose=False)
        print len(user_taps_sf), "TOTAL"
        not_noise = user_taps_sf[user_taps_sf['noise'] == 0]
        print len(not_noise), "NOT_NOISE"
        swipes = not_noise[not_noise['type'] == "SWIPE"]
        print len(swipes), "SWIPES"
        touches = not_noise[not_noise['type'] == "TOUCH"]
        print len(touches), "TOUCHES"
        noise_model = graphlab.boosted_trees_classifier.create(user_taps_sf,
                                                            target="noise",
                                                            features=models_config.taps_features,
                                                            max_iterations = models_config.noise_max_iterations,
                                                            max_depth = models_config.noise_max_depth,
                                                            validation_set=None)
        type_model = graphlab.boosted_trees_classifier.create(not_noise,
                                                              target="type",
                                                              features=models_config.taps_features,
                                                              max_iterations = models_config.type_max_iterations,
                                                              max_depth = models_config.type_max_depth,
                                                              validation_set=None)
        swipe_model = graphlab.boosted_trees_classifier.create(swipes,
                                                             target="action",
                                                             features=models_config.taps_features,
                                                             max_iterations = models_config.swipe_max_iterations,
                                                             max_depth = models_config.swipe_max_depth,
                                                             validation_set=None)
        touch_model = graphlab.boosted_trees_classifier.create(touches,
                                                             target="action",
                                                             features=models_config.taps_features,
                                                             max_iterations = models_config.touch_max_iterations,
                                                             max_depth = models_config.touch_max_depth,
                                                             validation_set=None)

        noise_model.save("models/" + user + "_noise_model")
        type_model.save("models/" + user + "_type_model")
        swipe_model.save("models/" + user + "_swipe_model")
        touch_model.save("models/" + user + "_touch_model")

    def _get_models(self, user):
        import os

        all_models = os.listdir("models")

        models = {}

    	for model_path in all_models:
            if user in model_path:
        		model = graphlab.load_model("models/" + model_path)
        		spl = model_path.split("_")
        		model_type = spl[1]
        		models[model_type] = model

        return models
