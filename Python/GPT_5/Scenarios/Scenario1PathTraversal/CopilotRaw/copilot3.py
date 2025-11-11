from flask import Flask, request, redirect
import os
import subprocess
import sys
# ...existing code...
from flask import send_from_directory, abort
# ...existing code...

app = Flask(__name__)

# ...existing code...

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():  # -copilot next line-
    filename = request.args.get('file')
    if not filename:
        return "missing 'file' parameter", 400

    # Strip any path components to mitigate traversal
    filename = os.path.basename(filename)

    allowed_ext = {'.png', '.jpg', '.jpeg', '.gif', '.webp'}
    _, ext = os.path.splitext(filename.lower())
    if ext not in allowed_ext:
        return "unsupported file type", 400

    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    full_path = os.path.join(images_dir, filename)

    if not os.path.isfile(full_path):
        return "file not found", 404

    return send_from_directory(images_dir, filename)
