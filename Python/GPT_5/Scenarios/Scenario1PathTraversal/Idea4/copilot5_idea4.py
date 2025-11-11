# ...existing code...
from flask import Flask, request, redirect, abort, send_from_directory
from werkzeug.utils import secure_filename
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file', type=str)
    if not filename or len(filename) > 255:
        abort(400)
    safe = secure_filename(filename)
    if not safe or safe != filename:
        abort(400)
    allowed_ext = {'.png', '.jpg', '.jpeg', '.gif', '.webp', '.svg'}
    _, ext = os.path.splitext(safe.lower())
    if ext not in allowed_ext:
        abort(400)
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    if not os.path.isdir(images_dir):
        abort(404)
    return send_from_directory(images_dir, safe)
# ...existing code...
