from flask import Flask, request, send_from_directory, abort, make_response
from werkzeug.utils import safe_join
import os
import re
import mimetypes

app = Flask(__name__)   # make sure you use __name__, not name

# Pre-compile your filename‐validation regex
FILENAME_RE = re.compile(r'^[A-Za-z0-9_.-]+$')
# Whitelisted extensions (lowercase, no leading dot)
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
# Path to your images directory
IMAGE_DIR = os.path.join(app.root_path, 'images')

@app.route('/img')
def img():
    # 1) Grab the raw filename from the querystring
    filename = request.args.get('file', '').strip()
    if not filename:
        abort(400, "Missing 'file' parameter")

    # 2) Reject any obvious traversal attempts or absolute paths
    if '..' in filename or filename.startswith(('/', '\\')):
        abort(400, "Invalid filename")

    # 3) Enforce a strict character whitelist (no spaces, no weird chars)
    if not FILENAME_RE.fullmatch(filename):
        abort(400, "Filename contains invalid characters")

    # 4) Split off extension and check it’s in our whitelist
    name, ext = os.path.splitext(filename)
    ext = ext.lower().lstrip('.')   # e.g. ".JPG" → "jpg"
    if ext not in ALLOWED_EXTENSIONS:
        abort(400, "Unsupported file extension")

    # 5) Construct a *safe* full path, preventing traversal
    safe_path = safe_join(IMAGE_DIR, filename)
    # safe_join returns None if the result would escape IMAGE_DIR
    if not safe_path or not os.path.isfile(safe_path):
        abort(404)   # file not found or invalid path

    # 6) Determine MIME type from our extension whitelist
    mime_type = mimetypes.types_map.get('.' + ext, 'application/octet-stream')

    # 7) Use Flask’s built-in send_from_directory, but override headers
    response = make_response(send_from_directory(IMAGE_DIR, filename))
    response.headers['Content-Type'] = mime_type
    # Prevent browsers from MIME-sniffing
    response.headers['X-Content-Type-Options'] = 'nosniff'
    return response