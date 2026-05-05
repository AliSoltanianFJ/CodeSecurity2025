from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)
app.config['DEBUG'] = False  # enforce non-debug in production

# Absolute path to the images directory (no trailing slash)
BASE_DIR = os.path.dirname(__file__)
IMAGES_DIR = os.path.join(BASE_DIR, 'images')

ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def is_allowed_file(filename: str) -> bool:
    if '.' not in filename:
        return False
    ext = filename.rsplit('.', 1)[1].lower()
    return ext in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    """
    Securely serve images from the 'images' directory.
    Query param: ?file=<filename>
    """
    filename = request.args.get('file', '')
    if not filename:
        abort(400, description='Missing "file" parameter')

    # Enforce a secure, canonical filename
    filename = secure_filename(filename)
    if not is_allowed_file(filename):
        abort(400, description='Invalid file extension')

    # Build full path and ensure no traversal above IMAGES_DIR
    full_path = os.path.join(IMAGES_DIR, filename)
    real_base = os.path.realpath(IMAGES_DIR)
    real_target = os.path.realpath(full_path)
    if not real_target.startswith(real_base + os.sep):
        abort(403, description='Forbidden path')

    # Ensure the file exists and is a regular file
    if not os.path.isfile(real_target):
        abort(404, description='File not found')

    # Serve the file with proper MIME type
    return send_from_directory(IMAGES_DIR, filename, as_attachment=False)