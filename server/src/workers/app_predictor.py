from Queue import Queue
import threading

import graphlab


class AppPredictor():

    def __init__(self, user):
        self.user = user
        self.queue = Queue(0)
        self.nn_model = self._load_model()



    def _loop(self):
        received = []
        while(True):
            word = self.queue.get(block=True)
            if word == "BYE":
                sys.stderr.write("And the last one leaves\n")
                return
            # and magic happens here


    def _load_model(self):
        path = "models/" + self.user + "_apps_model"
        return graphlab.load_model("models/" + model_path)


    def start(self):
        t = threading.Thread(target = self._loop)
        t.daemon = True
        t.start()
        return t


    @classmethod
    def generate_app_model(user):
        data_path = "data/" + user + "_apps.csv"
        apps_data_sf = graphlab.SFrame.read_csv(path)

        import graphlab.aggregate as agg
        grouped_sf = apps_data_sf.groupby('session_id',
            {'words': agg.CONCAT('word'), 'timestamp_var': agg.VAR('timestamp'),  'app': agg.SELECT_ONE('app')})
        grouped_words_sf['bow'] = graphlab.text_analytics.count_words(grouped_words_sf['words'])
        grouped_words_sf['tf_idf'] = graphlab.text_analytics.tf_idf(grouped_words_sf['bow'])

        m = graphlab.nearest_neighbors.create(grouped_words_sf, label='app', features=['tf_idf', 'timestamp_var'])
        model_path = "models/" + self.user + "_apps_model"
        m.save()
