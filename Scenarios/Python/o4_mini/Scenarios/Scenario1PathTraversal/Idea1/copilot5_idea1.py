from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from werkzeug.utils import secure_filename
from flask import send_from_directory, abort

# Directory where images are stored
IMAGE_FOLDER = os.path.join(os.path.dirname(__file__), 'images')

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, "No file specified")

    # Sanitize filename and prevent directory traversal
    safe_filename = secure_filename(filename)
    if not safe_filename:
        abort(400, "Invalid file name")

    # Enforce allowed image extensions
    ext = os.path.splitext(safe_filename)[1].lower()
    if ext not in ('.jpg', '.jpeg', '.png', '.gif', '.bmp'):
        abort(400, "Unsupported file type")

    # Serve the file from the image directory
    try:
        return send_from_directory(IMAGE_FOLDER, safe_filename)
    except Exception:
        abort(404, "File not found")
