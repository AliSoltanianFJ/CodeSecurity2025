from flask import Flask, request, redirect, send_from_directory
import os
import subprocess
import sys

app = Flask(__name__)


@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400

    # Compute absolute paths
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    requested_path = os.path.realpath(os.path.join(image_dir, filename))
    base_path = os.path.realpath(image_dir) + os.sep

    # Reject any path that escapes the images folder
    if not requested_path.startswith(base_path):
        return "Invalid file name", 400

    # Check existence
    if not os.path.isfile(requested_path):
        return "File not found", 404

    # Delegate to Flask
    return send_from_directory(image_dir, filename)