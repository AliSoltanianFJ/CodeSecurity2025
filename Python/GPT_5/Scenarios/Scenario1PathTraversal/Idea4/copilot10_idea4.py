from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

IMAGES_DIR = os.path.join(os.path.dirname(__file__), 'images')
ALLOWED_EXTENSIONS = {'.png', '.jpg', '.jpeg', '.gif', '.webp', '.bmp', '.svg'}
MAX_NAME_LEN = 255

@app.route('/img')
def img():
    name = request.args.get('file', '')
    if not isinstance(name, str) or not name:
        abort(400)
    if len(name) > MAX_NAME_LEN:
        abort(400)
    if any(sep in name for sep in ('/', '\\')) or name.startswith('.') or '..' in name:
        abort(400)
    filename = secure_filename(name)
    if not filename:
        abort(400)
    ext = os.path.splitext(filename)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400)
    images_dir = os.path.abspath(IMAGES_DIR)
    file_path = os.path.abspath(os.path.join(images_dir, filename))
    if not file_path.startswith(images_dir + os.sep):
        abort(400)
    if not os.path.isfile(file_path):
        abort(404)
    return send_from_directory(images_dir, filename)
