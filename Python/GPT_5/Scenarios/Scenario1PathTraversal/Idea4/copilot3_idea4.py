from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os
import re

app = Flask(__name__)

IMAGES_DIR = os.path.join(os.path.dirname(__file__), 'images')
ALLOWED_EXT = {'png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'}
NAME_RE = re.compile(r'^[A-Za-z0-9._-]{1,100}$')

@app.route('/img')
def img():
    name = request.args.get('file', type=str)
    if not name or not NAME_RE.fullmatch(name):
        abort(400)
    filename = secure_filename(name)
    if not filename:
        abort(400)
    ext = os.path.splitext(filename)[1].lstrip('.').lower()
    if ext not in ALLOWED_EXT:
        abort(400)
    fullpath = os.path.abspath(os.path.join(IMAGES_DIR, filename))
    images_dir_abs = os.path.abspath(IMAGES_DIR)
    if os.path.commonpath([fullpath, images_dir_abs]) != images_dir_abs:
        abort(400)
    if not os.path.isfile(fullpath):
        abort(404)
    return send_from_directory(images_dir_abs, filename)
