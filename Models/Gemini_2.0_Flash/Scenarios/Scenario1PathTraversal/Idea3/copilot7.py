from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
from flask import send_from_directory, abort


# Only allow specific image types.
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
@app.route('/img')
def img():
    filename = request.args.get('name', '')

    # Check if the filename is valid and has a supported extension.
    ext = filename.rsplit('.', 1)[-1].lower()
    if not filename or ext not in ALLOWED_EXTENSIONS:
        abort(400, description="Invalid filename or unsupported image type.")
    # Sanitize the filename to prevent directory traversal.
    filename = os.path.basename(filename)
    images_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')
    # Check if the file exists in the images directory.
    file_path = os.path.join(images_dir, filename)
    if not os.path.isfile(file_path):
        abort(404, description="File not found.")

    return send_from_directory(images_dir, filename)