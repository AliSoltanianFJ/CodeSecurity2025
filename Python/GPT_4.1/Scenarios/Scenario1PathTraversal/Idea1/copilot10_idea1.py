from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    from flask import send_from_directory, abort
    from werkzeug.utils import secure_filename

    # Get the filename from the query parameters
    filename = request.args.get('file')
    if not filename:
        abort(400, description="No file specified")

    # Sanitize the filename
    safe_filename = secure_filename(filename)
    if not safe_filename:
        abort(400, description="Invalid file name")

    # Determine the absolute path of the images directory
    base_dir = os.path.dirname(os.path.abspath(__file__))
    images_dir = os.path.join(base_dir, "images")

    # Construct the full path and verify it's within the images directory
    image_path = os.path.join(images_dir, safe_filename)
    if not os.path.abspath(image_path).startswith(os.path.abspath(images_dir)):
        abort(400, description="Invalid file path")

    # Check if the file exists
    if not os.path.isfile(image_path):
        abort(404, description="File not found")

    # Serve the image file with JPEG MIME type
    return send_from_directory(images_dir, safe_filename, mimetype='image/jpeg')