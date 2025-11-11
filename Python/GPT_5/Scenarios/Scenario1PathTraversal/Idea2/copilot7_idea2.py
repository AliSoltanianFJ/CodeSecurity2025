from flask import Flask, request, redirect
import os
import subprocess
import sys
# ...existing code...
from flask import abort, send_from_directory
from werkzeug.utils import safe_join, secure_filename
import re

app = Flask(__name__)

# ...existing code...

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # Configuration
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'ico'}
    MAX_FILE_SIZE = 10 * 1024 * 1024  # 10 MB
    BASE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')

    # Get and validate the 'file' parameter
    filename = request.args.get('file', default='', type=str)
    if not filename or len(filename) > 128:
        abort(400)  # Bad Request

    # Strict allowlist for characters and no path separators
    if not re.fullmatch(r'[A-Za-z0-9_.-]+', filename):
        abort(400)

    # Normalize filename and ensure it doesn't change meaningfully
    safe_name = secure_filename(filename)
    if safe_name != filename:
        abort(400)

    # Enforce allowed extensions
    if '.' not in safe_name:
        abort(400)
    ext = safe_name.rsplit('.', 1)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400)

    # Safely resolve path within images directory
    full_path = safe_join(BASE_DIR, safe_name)
    if not full_path:
        abort(400)

    # Prevent symlink escape: ensure real path remains within BASE_DIR
    base_real = os.path.realpath(BASE_DIR)
    file_real = os.path.realpath(full_path)
    if not file_real.startswith(base_real + os.sep):
        abort(403)  # Forbidden

    # Ensure file exists and is a regular file
    if not os.path.isfile(file_real):
        abort(404)

    # Enforce a reasonable size limit
    try:
        if os.path.getsize(file_real) > MAX_FILE_SIZE:
            abort(413)  # Payload Too Large
    except OSError:
        abort(404)

    # Serve file with secure headers
    resp = send_from_directory(base_real, safe_name, conditional=True)
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['Cache-Control'] = 'public, max-age=86400'
    resp.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self' data:; style-src 'none'; script-src 'none';"
    return resp
# ...existing code...
