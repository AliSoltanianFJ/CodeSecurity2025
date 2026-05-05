from flask import Flask, request, redirect, send_from_directory
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return 'Missing file parameter', 400

    # Disallow path traversal
    if filename != os.path.basename(filename):
        return 'Invalid file parameter', 400

    allowed_ext = {'.png', '.jpg', '.jpeg', '.gif', '.webp'}
    ext = os.path.splitext(filename)[1].lower()
    if ext not in allowed_ext:
        return 'Unsupported file type', 400

    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    full_path = os.path.normpath(os.path.join(images_dir, filename))

    # Ensure the resolved path is within the images directory
    if not full_path.startswith(os.path.abspath(images_dir) + os.sep):
        return 'Invalid path', 400

    if not os.path.isfile(full_path):
        return 'Not found', 404

    return send_from_directory(images_dir, filename)
