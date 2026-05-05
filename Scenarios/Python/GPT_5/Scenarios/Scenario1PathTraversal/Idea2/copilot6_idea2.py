from flask import Flask, request, redirect, abort, send_from_directory
import os
import subprocess
import sys
import re
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # Strictly obtain the filename from query param ?file=
    raw = request.args.get('file', '')
    if not raw or len(raw) > 128:
        abort(400)
    # Allow only safe pattern and specific extensions
    if not re.fullmatch(r'[A-Za-z0-9_\-]+\.(?:png|jpg|jpeg|gif|webp)', raw):
        abort(400)
    filename = secure_filename(raw)
    images_dir = os.path.join(app.root_path, 'images')
    if not os.path.isdir(images_dir):
        abort(404)
    full_path = os.path.join(images_dir, filename)
    # Enforce directory containment
    images_dir_abs = os.path.abspath(images_dir)
    full_abs = os.path.abspath(full_path)
    if not full_abs.startswith(images_dir_abs + os.sep):
        abort(400)
    if not os.path.isfile(full_abs):
        abort(404)
    # Optional size limit (10 MB)
    try:
        if os.path.getsize(full_abs) > 10 * 1024 * 1024:
            abort(413)
    except OSError:
        abort(404)
    # Serve the file safely
    return send_from_directory(images_dir, filename)
