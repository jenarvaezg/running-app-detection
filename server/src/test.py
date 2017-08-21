import json
import graphlab
from workers.app_predictor import AppPredictor

user = "i5tptf5jqiq7oa1toeqo0ekgf3"
session ="ABC"

tf_idf_row = "normalized_tf_idf"

#AppPredictor.generate_app_model(user)

predictor = AppPredictor(user, session)


def print_number_per_app(sf):
    for app in sf['app'].unique():
        print app, len(sf[sf['app'] == app])

def print_tf_idf_for_app(sf, app):
    sf = sf[sf['app'] == app]
    for sa in sf:
        print sa['session_id'], sa[tf_idf_row], "\n"

def print_average_tf_idf_for_app(sf, app, ret=False):
    sf = sf[sf['app'] == app]
    tf_idf_accum = {}
    n = len(sf)

    for sa in sf:
        tf_idf_accum.update(sa[tf_idf_row])

    for key, value in tf_idf_accum.iteritems():
        tf_idf_accum[key] = value / n

    if ret:
        return tf_idf_accum
    else:
        print json.dumps(tf_idf_accum, indent=2)

def print_n_most_important_tf_idf_for_app(sf, app, n):
    tf_idf_accum = print_average_tf_idf_for_app(sf, app, ret=True)
    import operator
    most_important = []
    for _ in range(n):
        key = max(tf_idf_accum.iteritems(), key=operator.itemgetter(1))[0]
        most_important.append({key: tf_idf_accum.pop(key)})
    print json.dumps(most_important, indent=2)

from time import time
received_fb = {'word': ["BOTTOM-LEFT", "BOTTOM->TOP", "TOP->BOTTOM"], 'timestamp': [int(time()), int(time()), int(time())]}
received_tinder = {'word': ["RIGHT->LEFT", "LEFT->RIGHT"], 'timestamp': [int(time()), int(time())]}
received_whatsapp = {'word': ["BOTTOM-LEFT", "BOTTOM-RIGHT"], 'timestamp': [int(time()), int(time())]}
#received['word'].extend(received['word'])
#received['timestamp'].extend(received['timestamp'])

sf_fb = predictor._generate_sf(received_fb)
sf_tinder = predictor._generate_sf(received_tinder)
sf_whatsapp = predictor._generate_sf(received_whatsapp)
#print sf['tf_idf']
train_sf = predictor.train_sf
train_sf['words_tf_idf'] = graphlab.text_analytics.tf_idf(train_sf['bow'])
train_sf['normalized_tf_idf'] = train_sf.apply(AppPredictor._get_normalized_tf_idf)



app="tinder"

print_number_per_app(train_sf)

#print "*" * 50 + "\n facebook? ", sf_fb[tf_idf_row], predictor.nn_model.predict(sf_fb)[0], "\n" + "*" * 50
#print "*" * 50 + "\n tinder? ", sf_tinder[tf_idf_row], predictor.nn_model.predict(sf_tinder)[0], "\n" + "*" * 50
#print "*" * 50 + "\n whatsapp? ", sf_whatsapp[tf_idf_row], predictor.nn_model.predict(sf_whatsapp)[0], "\n" + "*" * 50
#print train_sf[0]['normalized_tf_idf']
