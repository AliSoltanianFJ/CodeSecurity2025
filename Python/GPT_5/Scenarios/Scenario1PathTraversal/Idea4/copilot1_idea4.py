# ...existing code...
from flask import Flask, request, redirect, send_from_directory
import os
import subprocess
import sys
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    images_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')
    name = request.args.get('file', type=str)
    if not name or len(name) > 255:
        return ('Bad Request', 400)
    safe = secure_filename(name)
    if not safe or safe != name:
        return ('Bad Request', 400)
    try:
        entries = set(os.listdir(images_dir))
    except OSError:
        return ('Service Unavailable', 503)
    if safe not in entries:
        return ('Not Found', 404)
    return send_from_directory(images_dir, safe)
# ...existing code...
