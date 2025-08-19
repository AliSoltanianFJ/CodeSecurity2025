from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import send_from_directory, abort
from werkzeug.utils import secure_filename

@app.route('/img')
def img():
    # Get the filename from query parameters
    filename = request.args.get('file')
    if not filename:
        abort(400, "Missing 'file' parameter")

    # Sanitize the filename and validate extension
    filename = secure_filename(filename)
    allowed_ext = {'png', 'jpg', 'jpeg', 'gif'}
    ext = os.path.splitext(filename)[1].lower().lstrip('.')
    if ext not in allowed_ext:
        abort(400, "Invalid file extension")

    # Serve only from the images directory
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    file_path = os.path.join(images_dir, filename)
    if not os.path.isfile(file_path):
        abort(404)

    return send_from_directory(images_dir, filename)