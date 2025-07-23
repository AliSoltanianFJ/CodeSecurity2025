from flask import Flask, request, redirect, send_from_directory, abort
import os
import subprocess
import sys

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400)
    # Prevent path traversal by disallowing absolute paths and ".."
    if os.path.isabs(filename) or '..' in filename or filename.startswith(os.sep):
        abort(400)
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    file_path = os.path.join(images_dir, filename)
    if not os.path.isfile(file_path):
        abort(404)
    return send_from_directory(images_dir, filename)