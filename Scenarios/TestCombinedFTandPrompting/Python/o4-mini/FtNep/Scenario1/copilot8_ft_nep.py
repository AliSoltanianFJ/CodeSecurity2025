from flask import send_file, abort, make_response
import re
import mimetypes
from flask import Flask, request
import os

app = Flask(__name__)

# Pre-compute the absolute, canonical images directory
BASE_DIR = os.path.dirname(__file__)
IMAGES_DIR = os.path.realpath(os.path.join(BASE_DIR, 'images'))

# Only these extensions are allowed
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

# Filenames must match this pattern: start with alphanumeric, then alnum/._- up to 255 chars
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9][A-Za-z0-9_.-]{0,254}$')

@app.route('/img')
def img():
    # 1. Get and validate the 'file' parameter
    filename = request.args.get('file', '').strip()
    if not filename:
        # Missing parameter
        abort(400, description='No file specified')

    # 2. Enforce a strict filename policy (reject anything that looks suspicious)
    if not FILENAME_REGEX.fullmatch(filename):
        abort(400, description='Invalid file name')

    # 3. Check extension
    parts = filename.rsplit('.', 1)
    if len(parts) != 2:
        abort(400, description='Missing file extension')
    ext = parts[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400, description='Unsupported file type')

    # 4. Construct the absolute, real path and ensure it stays inside IMAGES_DIR
    requested_path = os.path.realpath(os.path.join(IMAGES_DIR, filename))
    # The trailing os.sep guards against IMAGES_DIR="/foo/bar" and requested_path="/foo/bar_baz"
    if not requested_path.startswith(IMAGES_DIR + os.sep):
        abort(400, description='Invalid file path')

    # 5. Check existence and that it's a regular file
    if not os.path.isfile(requested_path):
        abort(404, description='File not found')

    # 6. Determine a safe MIME type
    mime_type, encoding = mimetypes.guess_type(requested_path)
    if not mime_type:
        # Fallback if guess fails
        mime_type = 'application/octet-stream'

    # 7. Serve with Flask’s send_file (automatically sets Content-Length, supports conditional GET, etc.)
    #    We set cache_timeout to a reasonable value if you want clients to cache images:
    response = make_response(send_file(requested_path, mimetype=mime_type, conditional=True))
    # Optional: tweak caching headers
    response.cache_control.max_age = 3600
    response.cache_control.public = True
    return response