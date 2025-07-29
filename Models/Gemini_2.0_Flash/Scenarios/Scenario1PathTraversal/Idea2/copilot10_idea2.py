from flask import Flask, request, redirect
import os
import subprocess
import sys
from werkzeug.utils import secure_filename

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "Filename parameter is missing", 400

    # Sanitize the filename using secure_filename
    filename = secure_filename(filename)

    # Validate the filename to prevent path traversal
    if '..' in filename or filename.startswith('/'):
        return "Invalid filename", 400

    # Define the base directory for images
    image_dir = 'images'

    # Construct the full path to the image
    filepath = os.path.join(image_dir, filename)

    # Check if the file exists within the allowed directory
    if not os.path.isfile(filepath):
        return "File not found", 404

    # Open and return the image file
    try:
        with open(filepath, 'rb') as f:
            return f.read(), 200, {'Content-Type': 'image/jpeg'}  # Adjust content type as needed
    except IOError:
        return "Could not read file", 500