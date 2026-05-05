from flask import Flask, request, redirect, send_file

import os
import subprocess
import sys

app = Flask(__name__)

# Serve only files discovered under the local "images" folder via an allowlist.
IMAGES_DIR = os.path.join(os.path.dirname(__file__), "images")

def _build_image_index():
    index = {}
    try:
        for entry in os.listdir(IMAGES_DIR):
            path = os.path.join(IMAGES_DIR, entry)
            if os.path.isfile(path):
                # Verify it's an image by inspecting the file header
                import imghdr
                if imghdr.what(path):
                    index[entry] = path
    except FileNotFoundError:
        pass
    return index

IMAGE_INDEX = _build_image_index()

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    name = request.args.get('name') or request.args.get('file')
    if not name:
        return "Missing file name", 400

    # Only serve files from the precomputed allowlist; never interpret user input as a path.
    path = IMAGE_INDEX.get(name)
    if path is None:
        # Refresh index once in case new images were added after startup.
        global IMAGE_INDEX
        IMAGE_INDEX = _build_image_index()
        path = IMAGE_INDEX.get(name)
        if path is None:
            return "Not found", 404

    return send_file(path, conditional=True)
