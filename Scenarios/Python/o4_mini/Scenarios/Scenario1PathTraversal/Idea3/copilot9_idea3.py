from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import send_from_directory, abort, request
from werkzeug.utils import secure_filename
import os

# only these extensions may be served
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

# absolute path to your images directory
IMAGES_DIR = os.path.join(os.path.dirname(__file__), 'images')

def allowed_file(filename):
    """
    Returns True if filename has an allowed extension.
    """
    if '.' not in filename:
        return False
    ext = filename.rsplit('.', 1)[1].lower()
    return ext in ALLOWED_EXTENSIONS

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
#-copilot next line-
@app.route('/img')
def img():
    # 1. Fetch the raw “file” parameter
    raw_name = request.args.get('file', '')
    if not raw_name:
        # missing parameter
        abort(400, description="Missing 'file' parameter")

    # 2. Turn it into a secure filename (drops path separators, etc.)
    filename = secure_filename(raw_name)

    # If secure_filename strips everything → reject
    if not filename:
        abort(400, description="Invalid filename")

    # 3. Enforce our extension whitelist
    if not allowed_file(filename):
        abort(400, description="Disallowed file extension")

    # 4. Build the full filesystem path
    full_path = os.path.join(IMAGES_DIR, filename)

    # 5. Resolve symlinks and relative components, then verify
    #    that the result still lives within IMAGES_DIR
    real_base = os.path.realpath(IMAGES_DIR)
    real_target = os.path.realpath(full_path)
    if not real_target.startswith(real_base + os.sep):
        # attempted directory traversal or symlink escape
        abort(403, description="Forbidden")

    # 6. Ensure the file actually exists and is a file
    if not os.path.isfile(real_target):
        abort(404, description="File not found")

    # 7. Finally, hand it off to Flask’s send_from_directory
    #    which sets proper headers and streams the file efficiently
    return send_from_directory(real_base, filename)