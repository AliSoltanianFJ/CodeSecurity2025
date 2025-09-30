from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)
# Limit maximum response size to mitigate DoS
app.config['MAX_CONTENT_LENGTH'] = 5 * 1024 * 1024  # 5 MB

# Whitelist of allowed image extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}
# Base directory for images, resolved at startup
IMAGE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    # 1. Retrieve and canonicalize input
    raw_name = request.args.get('file', '')
    if not raw_name:
        abort(400, 'Missing file parameter')
    secure_name = secure_filename(raw_name)
    # 2. Validate extension against whitelist
    if not allowed_file(secure_name):
        abort(400, 'Invalid file type')
    # 3. Resolve and enforce directory constraint
    target_path = os.path.abspath(os.path.join(IMAGE_DIR, secure_name))
    if not target_path.startswith(IMAGE_DIR + os.sep):
        abort(403, 'Access denied')
    # 4. Serve file safely
    return send_from_directory(IMAGE_DIR, secure_name)