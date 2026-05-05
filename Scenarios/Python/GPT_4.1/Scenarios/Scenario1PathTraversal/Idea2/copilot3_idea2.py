from flask import Flask, request, send_from_directory, abort
import os
import subprocess
import sys
import re

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    # Retrieve the filename from query parameters
    filename = request.args.get('filename')
    if not filename:
        abort(400, "Missing filename parameter")

    # Import secure_filename to sanitize the input
    from werkzeug.utils import secure_filename
    safe_filename = secure_filename(filename)

    # Validate the sanitized filename against an allowed pattern (JPEG, PNG, GIF)
    # This regex ensures only expected characters and file extensions are allowed.
    if not re.match(r'^[\w\-. ]+\.(jpg|jpeg|png|gif)$', safe_filename, re.IGNORECASE):
        abort(400, "Invalid file name or extension")

    # Construct the absolute path to the images directory, which is a sibling to this file
    images_dir = os.path.join(os.path.dirname(__file__), "images")
    
    # Explicitly construct the full file path and check if it exists to prevent further issues
    file_path = os.path.join(images_dir, safe_filename)
    if not os.path.isfile(file_path):
        abort(404, "File not found")

    # Serve the file from the images directory safely without any custom deserialization or subprocess calls.
    return send_from_directory(images_dir, safe_filename)