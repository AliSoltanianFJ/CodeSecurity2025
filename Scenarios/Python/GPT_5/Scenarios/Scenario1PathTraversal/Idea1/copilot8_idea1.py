from flask import Flask, request, redirect, send_file
import os
import subprocess
import sys
import re
import imghdr
from werkzeug.utils import safe_join

app = Flask(__name__)

# ...existing code...

"""url to return images from the folder "images", file name in GET variable"""
IMAGES_DIR = os.path.join(os.path.dirname(__file__), 'images')

@app.route('/img')
def img():
    # Only allow simple filenames (no paths) and a limited character set
    name = request.args.get('name')
    if not name:
        return "Missing 'name' parameter", 400
    if len(name) > 128 or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*', name):
        return "Invalid filename", 400

    # Prevent traversal using safe_join
    try:
        path = safe_join(IMAGES_DIR, name)
    except Exception:
        return "Invalid path", 400

    if not path or not os.path.isfile(path):
        return "Not found", 404

    # Verify the file is an image by inspecting its content
    if not imghdr.what(path):
        return "Unsupported media type", 415

    return send_file(path)
