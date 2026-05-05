# ...existing code...
from flask import Flask, request, redirect, send_file, abort
import os
import subprocess
import sys

app = Flask(__name__)
# ...existing code...

# Pre-index allowed images at startup (no user-controlled paths used at request time)
ALLOWED_IMAGE_EXTS = {'.png', '.jpg', '.jpeg', '.gif', '.webp'}
IMAGES_DIR = os.path.join(app.root_path, 'images')

def _index_images():
    index = {}
    try:
        for entry in os.listdir(IMAGES_DIR):
            path = os.path.join(IMAGES_DIR, entry)
            if os.path.isfile(path):
                _, ext = os.path.splitext(entry)
                if ext.lower() in ALLOWED_IMAGE_EXTS:
                    index[entry] = path
    except FileNotFoundError:
        # Directory may not exist; serve nothing
        index = {}
    return index

IMAGES_INDEX = _index_images()

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():  # -copilot next line-
    name = request.args.get('name', type=str)
    if not name:
        abort(400, description="Missing 'name' parameter")

    # Only serve files that were pre-indexed (prevents traversal and arbitrary paths)
    path = IMAGES_INDEX.get(name)
    if not path:
        abort(404)

    return send_file(path, conditional=True)
# ...existing code...
