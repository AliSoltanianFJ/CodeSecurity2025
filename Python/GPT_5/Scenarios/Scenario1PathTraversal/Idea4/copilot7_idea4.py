# ...existing code...
from flask import Flask, request, send_from_directory, abort
import os
from werkzeug.utils import secure_filename
from werkzeug.exceptions import NotFound

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    name = request.args.get('file', type=str)
    if not name or len(name) > 255:
        abort(400)
    filename = secure_filename(name)
    if not filename:
        abort(400)
    ext = os.path.splitext(filename)[1].lower().lstrip('.')
    allowed = {'png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'}
    if ext not in allowed:
        abort(400)
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    try:
        return send_from_directory(images_dir, filename)
    except NotFound:
        abort(404)
# ...existing code...
