from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename
import os
import re

app = Flask(__name__)

# Define where your images live (absolute path)
BASE_DIR = os.path.abspath(os.path.dirname(__file__))
IMAGES_DIR = os.path.join(BASE_DIR, 'images')

# Only these extensions are allowed
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

# Strict regex: one filename component [A–Z,a–z,0–9,_-], 
# a single dot, then one of the allowed extensions
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9_-]+\.(png|jpg|jpeg|gif)$')

@app.route('/img')
def img():
    # 1) Get parameter
    filename = request.args.get('file', '')
    if not filename:
        abort(400, description='No file specified')

    # 2) Reject anything that doesn't exactly match our strict regex
    #    This blocks directory traversal, extra dots, slashes, etc.
    if not FILENAME_REGEX.match(filename):
        abort(400, description='Invalid file name or extension')

    # 3) Further sanitize via werkzeug.secure_filename
    #    secure_filename will strip any unsupported chars
    secure_name = secure_filename(filename)
    #    It must round-trip to the same string, otherwise reject
    if secure_name != filename:
        abort(400, description='Invalid file name after sanitization')

    # 4) Build the absolute path and ensure it stays inside IMAGES_DIR
    #    Use commonpath to prevent ../ escapes
    requested_path = os.path.join(IMAGES_DIR, secure_name)
    #    Resolve symlinks/relative components
    abs_requested = os.path.abspath(requested_path)
    if os.path.commonpath([IMAGES_DIR, abs_requested]) != IMAGES_DIR:
        abort(403, description='Forbidden')

    # 5) Verify the file exists and is a file
    if not os.path.isfile(abs_requested):
        abort(404, description='File not found')

    # 6) Serve the file (Flask will set Content-Type by extension)
    #    as_attachment=False means it will display inline where supported
    return send_from_directory(
        IMAGES_DIR,
        secure_name,
        as_attachment=False
    )
