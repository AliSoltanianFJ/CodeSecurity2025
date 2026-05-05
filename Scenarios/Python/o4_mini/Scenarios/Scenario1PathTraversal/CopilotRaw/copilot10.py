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
        return 'No file specified', 400

    # directory where images are stored
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    images_dir = os.path.abspath(images_dir)

    # construct the absolute path and normalize it
    requested_path = os.path.abspath(os.path.join(images_dir, filename))

    # prevent path traversal by ensuring the requested path is inside images_dir
    if not requested_path.startswith(images_dir + os.sep):
        return 'Invalid file path', 400

    if not os.path.exists(requested_path):
        return 'File not found', 404

    # safely serve the file
    return send_from_directory(images_dir, filename)