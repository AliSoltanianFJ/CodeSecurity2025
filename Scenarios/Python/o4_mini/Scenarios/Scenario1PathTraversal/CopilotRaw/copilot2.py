from flask import Flask, request, send_from_directory, abort
import os

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, 'Missing file parameter')
    # prevent basic path traversal
    if '..' in filename or filename.startswith(('/', '\\')):
        abort(400, 'Invalid filename')
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    return send_from_directory(images_dir, filename)