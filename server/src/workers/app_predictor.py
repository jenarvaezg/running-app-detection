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
            print current_sf['tf_idf']
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
        corpus_sf['tf_idf'] = graphlab.text_analytics.tf_idf(corpus_sf['bow'])

        return corpus_sf.tail(1)


    @classmethod
    def generate_app_model(cls, user):
        grouped_words_sf = cls._get_training_sf(user)
        grouped_words_sf['tf_idf'] = graphlab.text_analytics.tf_idf(grouped_words_sf['bow'])

        m = graphlab.classifier.create(grouped_words_sf, target='app', features=models_config.apps_features)
        model_path = "models/" + user + "_apps_model"
        m.save(model_path)


    @classmethod
    def _get_grouped_sf(cls, sf):
        import graphlab.aggregate as agg
        groupby_dict = {
        'words': agg.CONCAT('word'),
        'timestamp_var': agg.VAR('timestamp'),
        'timestamp_std': agg.STD('timestamp')}
        if "app" in sf.column_names():
            groupby_dict['app'] = agg.SELECT_ONE('app')

        grouped_words_sf = sf.groupby('session_id', groupby_dict)
        grouped_words_sf['bow'] = graphlab.text_analytics.count_words(grouped_words_sf['words'])

        return grouped_words_sf

    @classmethod
    def _get_training_sf(cls, user):
        data_path = "data/" + user + "_apps.csv"
        apps_data_sf = graphlab.SFrame.read_csv(data_path, verbose=False)

        grouped_words_sf = cls._get_grouped_sf(apps_data_sf)

        return grouped_words_sf
