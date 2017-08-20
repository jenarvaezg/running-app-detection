from Queue import Queue
import threading
import graphlab

from config import models_config


class AppPredictor():

    def __init__(self, user, session_id):
        self.user = user
        self.queue = Queue(0)
        self.session_id = session_id
        self.nn_model = self._load_model()
        self.train_sf = self._get_training_sf(user)

    def _loop(self):
        received = {'word': [], 'timestamp': []}
        while(True):
            msg = self.queue.get(block=True)
            if type(msg) == str:
                print "App predictor leaves"
                return

            received['word'].append(msg['word'])
            received['timestamp'].append(msg['timestamp'])
            current_sf = self._generate_sf(received)
            print current_sf['words_tf_idf']
            closest = self.nn_model.predict(current_sf)[0]
            print "********************"
            print "I predict the app is:", closest
            print "********************"


    def start(self):
        t = threading.Thread(target = self._loop)
        t.daemon = True
        t.start()
        return t


    def _load_model(self):
        model_path = "models/" + self.user + "_apps_model"
        return graphlab.load_model(model_path)



    def _generate_sf(self, received):
        received['session_id'] = [self.session_id for _ in range(len(received['word']))]
        sf = graphlab.SFrame(received)
        grouped_sf = self._get_grouped_sf(sf)
        train_sf = self.train_sf.copy()
        del train_sf['app']

        corpus_sf = train_sf.append(grouped_sf)
        corpus_sf['words_tf_idf'] = graphlab.text_analytics.tf_idf(corpus_sf['bow'])
        corpus_sf['normalized_tf_idf'] = corpus_sf.apply(AppPredictor._get_normalized_tf_idf)

        return corpus_sf.tail(1)


    @staticmethod
    def _get_normalized_tf_idf(sf):
        n = len(sf['words'])
        tf_idf_accum = {}

        for key, value in sf['words_tf_idf'].iteritems():
            tf_idf_accum[key] = value / n

        return tf_idf_accum


    @classmethod
    def generate_app_model(cls, user):
        grouped_words_sf = cls._get_training_sf(user)
        grouped_words_sf['words_tf_idf'] = graphlab.text_analytics.tf_idf(grouped_words_sf['bow'])
        grouped_words_sf['normalized_tf_idf'] = grouped_words_sf.apply(AppPredictor._get_normalized_tf_idf)


        m = graphlab.classifier.create(grouped_words_sf, target='app', features=models_config.apps_features)
        model_path = "models/" + user + "_apps_model"
        m.save(model_path)


    @classmethod
    def _aggregate_by_session_id(cls, sf):
        import numpy as np
        out_sf = graphlab.SFrame()

        has_app = "app" in sf.column_names()

        for session_id in sf['session_id'].unique():
            session_out_sf = graphlab.SFrame()
            session_out_sf['session_id'] = [session_id]
            words = []
            time_diffs = []
            sf_group = sf[sf['session_id'] == session_id]
            n_events = len(sf_group)

            for i in range(n_events -1, -1, -1):
                this_sf = sf_group[i]
                words.append(this_sf['word'])
                if i != 0:
                    time_diffs.append(this_sf['timestamp'] - sf_group[i-1]['timestamp'])

            session_out_sf['time_diff_mean'] = [np.mean(time_diffs)]
            session_out_sf['time_diff_var'] = [np.var(time_diffs)]
            session_out_sf['words'] = [words]

            if has_app:
                session_out_sf['app'] = [sf_group['app'][0]]

            out_sf = out_sf.append(session_out_sf)

        return out_sf


    @classmethod
    def _get_grouped_sf(cls, sf):
        grouped_words_sf = cls._aggregate_by_session_id(sf)
        grouped_words_sf['bow'] = graphlab.text_analytics.count_words(grouped_words_sf['words'])
        grouped_words_sf['swipe_percentage'] = grouped_words_sf['bow'].apply(_get_swipe_percentage)

        return grouped_words_sf

    @classmethod
    def _get_training_sf(cls, user):
        data_path = "data/" + user + "_apps.csv"
        apps_data_sf = graphlab.SFrame.read_csv(data_path, verbose=False)

        grouped_words_sf = cls._get_grouped_sf(apps_data_sf)

        return grouped_words_sf


def _get_swipe_percentage(bow):
    n = 0
    n_swipes = 0
    for word, times in bow.iteritems():
        n += times
        if "->" in word:
            n_swipes += times
    return n_swipes / float(n)
