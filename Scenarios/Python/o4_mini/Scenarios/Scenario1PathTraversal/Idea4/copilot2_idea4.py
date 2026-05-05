from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

import os
from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Base directory for images (never serve outside this)
IMAGES_DIR = os.path.join(os.path.dirname(__file__), 'images')
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    # 1. Fetch and validate parameter
    raw = request.args.get('file', '')
    if not raw:
        abort(400, 'Missing file parameter')

    # 2. Secure the filename and enforce extension whitelist
    filename = secure_filename(raw)
    if not allowed_file(filename):
        abort(400, 'Invalid file name or extension')

    # 3. Build and canonicalize absolute path
    requested = os.path.join(IMAGES_DIR, filename)
    abs_path = os.path.realpath(requested)
    base_real = os.path.realpath(IMAGES_DIR) + os.sep

    # 4. Enforce path confinement
    if not abs_path.startswith(base_real):
        abort(403)

    # 5. Verify the file exists
    if not os.path.isfile(abs_path):
        abort(404)

    # 6. Safely serve the file
    return send_from_directory(IMAGES_DIR, filename)

# ...existing code...