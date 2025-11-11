# ...existing code...
from flask import Flask, request, redirect, send_from_directory, abort
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

@app.route('/img')
def img():
    name = request.args.get('file')
    if not name or not isinstance(name, str):
        abort(400)
    if len(name) > 255:
        abort(400)
    filename = secure_filename(name)
    if not filename:
        abort(400)
    allowed_exts = {'.png', '.jpg', '.jpeg', '.gif', '.webp'}
    _, ext = os.path.splitext(filename.lower())
    if ext not in allowed_exts:
        abort(400)
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    try:
        return send_from_directory(images_dir, filename)
    except Exception:
        abort(404)
# ...existing code...
