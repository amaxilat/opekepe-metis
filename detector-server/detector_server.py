import gzip
import json
import os
import tensorflow as tf
import numpy as np
from flask import Flask, request, make_response, jsonify
from metrics import BinaryTP, BinaryFP, BinaryTN, BinaryFN
from tensorflow import keras
import time
import logging
from PIL import Image

logger_waitress = logging.getLogger('waitress')
logger_waitress.setLevel(logging.INFO)
logger_tf = logging.getLogger('tensorflow')
logger_tf.setLevel(logging.INFO)
logger = logging.getLogger('server')
logger.setLevel(logging.DEBUG)

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'

CONTENT_ENCODING_KEY = 'Content-Encoding'

app = Flask('__name__')

model_name = 'mode_64_20220805155009'
prod_model_name = 'model'
if os.path.exists(prod_model_name):
    model_name = prod_model_name

threshold = 0.8
model = keras.models.load_model(model_name, custom_objects={
    "BinaryTP": BinaryTP(),
    "BinaryFP": BinaryFP(),
    "BinaryTN": BinaryTN(),
    "BinaryFN": BinaryFN()
})

t_load = []
t_prepare = []
t_predict = []
t_reply = []


def avg(lst):
    if len(lst) == 0:
        return 0
    return sum(lst) / len(lst)


def load_request_data(req):
    compressed = False
    if CONTENT_ENCODING_KEY in req.headers and req.headers[CONTENT_ENCODING_KEY] == 'gzip':
        compressed = True
    logger.debug(
        f"path: {req.path} type: {req.headers['Content-Type']} size: {req.headers.get('Content-Length')} compressed: {compressed}")
    if compressed:
        return json.loads(gzip.decompress(req.data))
    else:
        return json.loads(req.data)


@app.route('/ping', methods=['GET'])
def get_ping():
    stats_response = {'load': avg(t_load), 'prepare': avg(t_prepare), 'predict': avg(t_predict), 'reply': avg(t_reply)}
    ping_response = {'model': model_name, 'threshold': threshold, 'stats': stats_response}
    return make_response(jsonify(ping_response), 200)


@app.route('/', methods=['POST'])
def detect_in_single_tile():
    n = time.time()
    # load
    request_data = load_request_data(request)
    t_load.append((time.time() - n))
    (h, w, predictions_bin, c, d) = predict_tile(request_data)
    return make_response(jsonify({'predictions': predictions_bin, 'h': h, 'w': w}), 200)


@app.route('/list', methods=['POST'])
def detect_in_many_tiles():
    n = time.time()
    # load
    request_data = load_request_data(request)
    t_load.append((time.time() - n))
    response_data = []
    for request_item in request_data['tiles']:
        (h, w, predictions_bin, c, d) = predict_tile(request_item)
        response_data.append({'predictions': predictions_bin, 'h': h, 'w': w})
    logger.debug(
        f'[{model_name}|{threshold}] load:{avg(t_load)} prepare:{avg(t_prepare)} predict:{avg(t_predict)} reply:{avg(t_reply)}')
    return make_response(jsonify({'tiles': response_data}), 200)


@app.route('/image', methods=['POST'])
def detect_in_image():
    request_data = load_request_data(request)
    image_file = request_data['image']
    mask_file = request_data['mask']
    logger.debug(f'image: {image_file}')
    logger.debug(f'mask: {mask_file}')
    (pixels, cloudy_pixels, percentage) = detect_clouds_in_image_file(image_file, mask_file)
    response_data = {
        'image': image_file,
        'mask': mask_file,
        'pixels': pixels,
        'cloudy': cloudy_pixels,
        'percentage': percentage
    }
    return make_response(jsonify(response_data), 200)


def detect_clouds_in_image_file(image_file, mask_file):
    total_start = time.time()

    image_data_array = np.array(Image.open(image_file))

    (im_height, im_width, num_components) = image_data_array.shape

    im_height_parts = int(im_height / 256)
    im_width_parts = int(im_width / 256)

    mask_img = np.zeros([im_height, im_width], dtype=np.uint8)
    mask_img.fill(40)
    mask = Image.fromarray(mask_img, mode='L')
    mask.save(mask_file)

    total_pixels = im_width * im_height
    cloudy_pixels = 0

    for h in range(im_height_parts):
        for w in range(im_width_parts):
            tile_data = []
            for t_h in range(256):
                for t_w in range(256):
                    # check if we need to take into account the nir value
                    tile_data.extend(image_data_array[h * 256 + t_h][w * 256 + t_w][0:4])
                    # print(image_data_array[h * 256 + t_h][w * 256 + t_w][0:4])
            tile = {'data': tile_data, 'h': h, 'w': w}
            (a1, a2, a3, mask_img, tile_cloud_pixels) = predict_tile(tile, mask_img)
            cloudy_pixels = cloudy_pixels + tile_cloud_pixels
        mask = Image.fromarray(mask_img, mode='L')
        mask.save(
            mask_file)

    total = time.time() - total_start
    logger.debug(f'total time: {total}')
    return total_pixels, cloudy_pixels, cloudy_pixels / total_pixels


def predict_tile(tile, mask_img=None):
    h = tile['h']
    w = tile['w']

    n = time.time()
    t_load.append((time.time() - n))
    # print('jsonload:', (time.time() - n))

    n = time.time()
    # prepare
    del tile['data'][3::4]
    rd = [float(i) / 255.0 for i in tile['data']]
    aa = [rd[i:i + 3] for i in range(0, len(rd), 3)]
    oo = [aa[i:i + 256] for i in range(0, len(aa), 256)]
    # rank_3_tensor = tf.Tensor(o, dtype=tf.float32)
    rank_3_tensor = tf.convert_to_tensor([oo])
    # print('prepare', (time.time() - n))
    t_prepare.append((time.time() - n))

    n = time.time()
    # predict
    predictions = model.predict(rank_3_tensor, verbose=0).tolist()[0]
    # print('predict', (time.time() - n))
    t_predict.append((time.time() - n))

    n = time.time()

    cloudy_pixels = 0

    # reply
    predictions_bin = []
    for x in range(256):
        predictions_row = []
        for y in range(256):
            cloud_prediction = 1 if predictions[x][y][0] > threshold and (
                    oo[x][y][0] > 0 or oo[x][y][1] > 0 or oo[x][y][2] > 0) and (
                                            oo[x][y][0] < 255 or oo[x][y][1] < 255 or oo[x][y][2] < 255) else 0
            predictions_row.append(cloud_prediction)
            if mask_img is not None:
                if cloud_prediction == 1:
                    mask_img[h * 256 + x][w * 256 + y] = 255
                    cloudy_pixels = cloudy_pixels + 1
                else:
                    mask_img[h * 256 + x][w * 256 + y] = 0
        predictions_bin.append(predictions_row)
    # print('reply', (time.time() - n))
    t_reply.append((time.time() - n))
    logger.debug(
        f'[{model_name}|{threshold}] ({h},{w}) load:{avg(t_load)} prepare:{avg(t_prepare)} predict:{avg(t_predict)} reply:{avg(t_reply)} cloudy: {cloudy_pixels}')
    return h, w, predictions_bin, mask_img, cloudy_pixels


if __name__ == '__main__':
    from waitress import serve

    serve(app, host='0.0.0.0', port=5000)
