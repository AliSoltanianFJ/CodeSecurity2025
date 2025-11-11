from flask import Flask, request, redirect, send_from_directory, abort
import os
import subprocess
import sys
from werkzeug.utils import secure_filename
# ...existing code...
app = Flask(__name__)
# ...existing code...
"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file', type=str)
    if not filename:
        return abort(400, description="Missing 'file' query parameter")

    filename = secure_filename(filename)
    if not filename or '.' not in filename:
        return abort(400, description="Invalid filename")

    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'}
    ext = filename.rsplit('.', 1)[1].lower()
    if ext not in allowed_exts:
        return abort(400, description="Unsupported file type")

    images_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')
    return send_from_directory(images_dir, filename)
