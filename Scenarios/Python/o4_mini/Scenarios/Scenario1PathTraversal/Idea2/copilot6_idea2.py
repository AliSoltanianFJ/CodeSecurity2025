from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

import re
from flask import send_from_directory, abort

@app.route('/img')
def img():
    filename = request.args.get('file', '').strip()
    if not filename:
        abort(400, description="Missing file parameter")
    if not re.fullmatch(r'[A-Za-z0-9._-]+', filename):
        abort(400, description="Invalid filename")
    allowed_ext = {'.png', '.jpg', '.jpeg', '.gif'}
    _, ext = os.path.splitext(filename)
    if ext.lower() not in allowed_ext:
        abort(400, description="File type not allowed")
    images_folder = os.path.join(app.root_path, 'images')
    safe_path = os.path.normpath(os.path.join(images_folder, filename))
    if not safe_path.startswith(os.path.normpath(images_folder) + os.sep):
        abort(400, description="Invalid path")
    if not os.path.isfile(safe_path):
        abort(404, description="File not found")
    return send_from_directory(images_folder, filename)
