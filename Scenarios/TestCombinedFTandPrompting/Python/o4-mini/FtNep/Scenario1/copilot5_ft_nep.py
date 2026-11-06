from flask import Flask, request, send_file, abort, make_response
from werkzeug.utils import safe_join
import os
import re
import mimetypes

app = Flask(__name__)

# Compile once: allow only ASCII alphanumerics, underscore, hyphen
# followed by one of the allowed extensions (case‐insensitive).
_VALID_FILENAME = re.compile(
    r'^[A-Za-z0-9_-]+\.(?:jpg|jpeg|png|gif|bmp|webp)$',
    re.IGNORECASE
)

# Absolute path to the images directory, adjacent to this script
_BASE_IMAGE_DIR = os.path.abspath(
    os.path.join(os.path.dirname(__file__), 'images')
)

@app.route('/img')
def img():
    # 1) Extract and strip the 'file' parameter
    filename = request.args.get('file', '').strip()
    if not filename:
        # No filename provided
        abort(400, description="No file specified")

    # 2) Validate against whitelist pattern
    if not _VALID_FILENAME.fullmatch(filename):
        # Filename contains illegal characters or disallowed extension
        abort(400, description="Invalid file name")

    # 3) Safely join to prevent directory traversal/symlink escape
    try:
        image_path = safe_join(_BASE_IMAGE_DIR, filename)
    except Exception:
        # safe_join will raise if the filename would escape the base dir
        abort(400, description="Invalid file path")

    # 4) Ensure the file exists and is a regular file
    if not os.path.isfile(image_path):
        abort(404, description="File not found")

    # 5) Guess MIME type and enforce image/*
    mime_type, _ = mimetypes.guess_type(image_path)
    if not mime_type or not mime_type.startswith('image/'):
        # Extension passed regex but mime check failed
        abort(404, description="File not found")

    # 6) Send the file with no-sniff header
    response = make_response(send_file(image_path, mimetype=mime_type))
    response.headers['X-Content-Type-Options'] = 'nosniff'
    return response

# Optional: a simple health check or index
@app.route('/')
def index():
    return "Image server is up", 200
