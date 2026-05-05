from flask import Flask, request, redirect
import os
import subprocess
import sys
import re
from flask import send_file, abort

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # Strictly fetch and validate the 'name' query parameter
    filename = request.args.get('name', '')
    if not filename or len(filename) > 100:
        abort(400)

    # Reject any suspicious characters (prevent path traversal / injection)
    if not re.fullmatch(r'[A-Za-z0-9_.\-]+', filename):
        abort(400)

    # Enforce allowed extensions
    allowed_ext = {'png', 'jpg', 'jpeg', 'gif', 'webp'}
    parts = filename.rsplit('.', 1)
    if len(parts) != 2 or parts[1].lower() not in allowed_ext:
        abort(400)

    # Build absolute, canonical path and ensure containment
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))
    file_path = os.path.abspath(os.path.join(base_dir, filename))
    if not file_path.startswith(base_dir + os.sep):
        abort(400)

    # Ensure file exists and is a regular file
    if not (os.path.isfile(file_path) and os.path.getsize(file_path) > 0):
        abort(404)

    # Serve file safely; conditional=True enables efficient client caching
    return send_file(file_path, conditional=True)
