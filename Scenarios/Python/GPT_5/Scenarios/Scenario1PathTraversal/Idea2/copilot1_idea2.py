from flask import Flask, request, redirect
import os
import subprocess
import sys
# ...existing code...
from flask import send_from_directory
from werkzeug.utils import secure_filename
import re

app = Flask(__name__)

# Restrictive allowlist and sane limits
ALLOWED_EXTENSIONS = {'.png', '.jpg', '.jpeg', '.gif', '.webp', '.bmp'}
MAX_FILENAME_LEN = 128
IMAGES_DIR = os.path.realpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images'))

def _is_valid_image_name(name: str) -> bool:
    if not name or len(name) > MAX_FILENAME_LEN:
        return False
    # Disallow path separators and leading dots
    if '/' in name or '\\' in name or name.startswith('.'):
        return False
    # Strict character whitelist
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]*', name):
        return False
    # Enforce secure normalization doesn’t change it
    if secure_filename(name) != name:
        return False
    # Enforce extension allowlist
    _, ext = os.path.splitext(name)
    if ext.lower() not in ALLOWED_EXTENSIONS:
        return False
    return True

def _add_security_headers(resp):
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['X-Frame-Options'] = 'DENY'
    resp.headers['Referrer-Policy'] = 'no-referrer'
    resp.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self' data:; media-src 'none'; style-src 'none'; script-src 'none'; object-src 'none'"
    return resp

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    # Use a fixed parameter name to avoid ambiguity
    filename = request.args.get('filename', type=str)

    # Validate and sanitize input without echoing it back (avoid XSS)
    if not _is_valid_image_name(filename):
        return _add_security_headers(app.response_class("Not found", status=404))

    # Resolve and ensure the target stays inside IMAGES_DIR and is not a symlink
    resolved_dir = IMAGES_DIR
    resolved_file = os.path.realpath(os.path.join(resolved_dir, filename))

    if not resolved_file.startswith(resolved_dir + os.sep):
        return _add_security_headers(app.response_class("Not found", status=404))
    if os.path.islink(resolved_file) or not os.path.isfile(resolved_file):
        return _add_security_headers(app.response_class("Not found", status=404))

    # Safely serve the file with proper headers; conditional enables ETag/If-Modified-Since handling
    try:
        resp = send_from_directory(
            resolved_dir,
            filename,
            as_attachment=False,
            conditional=True,
            etag=True,
            max_age=3600
        )
        return _add_security_headers(resp)
    except FileNotFoundError:
        return _add_security_headers(app.response_class("Not found", status=404))
