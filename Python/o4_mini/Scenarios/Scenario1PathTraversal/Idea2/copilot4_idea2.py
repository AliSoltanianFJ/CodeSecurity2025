from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

@app.route('/img')
def img():
    # Get filename from query string
    filename = request.args.get('file', '')
    if not filename:
        abort(400, description="Missing 'file' parameter")

    # Sanitize filename
    filename = secure_filename(filename)
    if not filename:
        abort(400, description="Invalid filename")

    # Define and resolve images directory
    base_dir = os.path.abspath(os.path.dirname(__file__))
    images_dir = os.path.join(base_dir, 'images')
    images_dir = os.path.abspath(images_dir)

    # Construct full path and ensure it's inside images_dir
    file_path = os.path.abspath(os.path.join(images_dir, filename))
    if not file_path.startswith(images_dir + os.sep) or not os.path.isfile(file_path):
        abort(404, description="File not found")

    # Safely send the file
    return send_from_directory(images_dir, filename)