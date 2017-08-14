from Queue import Queue
import threading

import graphlab
import config.models_config as models_config


class AppPredictor():

    def __init__(self, user):
        self.user = user
        self.queue = Queue(0)
        self.nn_model = self._load_model()


    def _loop(self):
        received = {'word': [], 'time': []}
        while(True):
            msg = self.queue.get(block=True)
            if type(msg) == str:
                sys.stderr.write("And the last one leaves\n")
                return

            received['word'].append(msg['word'])
            received['timestamp'].append(msg['timestamp'])
            current_sf = self._generate_sf(received)

            closest = self.nn_model.predict(current_sf)[0]
            print "********************"
            print "I predict the app is:", closest['reference_label']
            print "********************"

    def _load_model(self):
        path = "models/" + self.user + "_apps_model"
        return graphlab.load_model("models/" + model_path)

    def _generate_sf(self, received):
        sf = graphlab.SFrame(received)
        return self.get_sf_with_tfidf(sf)


    def start(self):
        t = threading.Thread(target = self._loop)
        t.daemon = True
        t.start()
        return t


    @classmethod
    def get_sf_with_tfidf(cls, sf):
        import graphlab.aggregate as agg
        groupby_dict = {
            'words': agg.CONCAT('word'),
            'timestamp_var': agg.VAR('timestamp'),
            'timestamp_std': agg.STD('timestamp')}
        if "app" in sf.column_names():
            groupby_dict['app'] = agg.SELECT_ONE('app')

        grouped_words_sf = sf.groupby('session_id', groupby_dict)
        grouped_words_sf['bow'] = graphlab.text_analytics.count_words(grouped_words_sf['words'])
        grouped_words_sf['tf_idf'] = graphlab.text_analytics.tf_idf(grouped_words_sf['bow'])

        return grouped_words_sf

    @classmethod
    def generate_app_model(cls, user):
        data_path = "data/" + user + "_apps.csv"
        apps_data_sf = graphlab.SFrame.read_csv(data_path)

        grouped_words_sf = cls.get_sf_with_tfidf(apps_data_sf)

        m = graphlab.classifier.create(grouped_words_sf, target='app', features=models_config.apps_features)
        model_path = "models/" + user + "_apps_model"
        m.save(model_path)
