import os
import re
import logging
from flask import Flask, request, send_from_directory, abort, make_response

# ---------------------
# Configuration
# ---------------------

# Base directory of this script
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# Directory where images are stored, must exist and not be writable by the web user
IMAGE_DIR = os.path.join(BASE_DIR, 'images')

# Allowed filename pattern: 1–50 chars of [A-Za-z0-9_-], plus a single extension
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9_-]{1,50}\.(?:jpg|jpeg|png|gif)$')

# Map extensions to MIME
MIME_TYPES = {
    '.jpg':  'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.png':  'image/png',
    '.gif':  'image/gif'
}

# Application factory
app = Flask(__name__)

# Never run in debug or with exception propagation to client
app.config.update({
    'DEBUG': False,
    'ENV': 'production',
    'PROPAGATE_EXCEPTIONS': False,
    'TRAP_HTTP_EXCEPTIONS': False
})

# Set up basic logging to a file (no PII), rotating at 5MB
handler = logging.handlers.RotatingFileHandler(
    os.path.join(BASE_DIR, 'app.log'),
    maxBytes=5*1024*1024,
    backupCount=3
)
handler.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler.setFormatter(formatter)
app.logger.addHandler(handler)
app.logger.setLevel(logging.INFO)

# ---------------------
# Helpers
# ---------------------

def is_safe_filename(filename):
    """
    Returns True if filename matches our strict whitelist regex.
    """
    return bool(FILENAME_REGEX.fullmatch(filename))

def get_mimetype(filename):
    """
    Choose the correct MIME type for an allowed extension.
    """
    ext = os.path.splitext(filename)[1].lower()
    return MIME_TYPES.get(ext)

# Global after_request to append security headers
@app.after_request
def set_security_headers(response):
    # Prevent MIME-type sniffing
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Very restrictive CSP: only allow self for images
    response.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self'"
    # Clickjacking protection
    response.headers['X-Frame-Options'] = 'DENY'
    return response

# ---------------------
# Error Handlers
# ---------------------

@app.errorhandler(400)
def bad_request(e):
    return make_response('Bad request', 400)

@app.errorhandler(404)
def not_found(e):
    return make_response('Not found', 404)

@app.errorhandler(500)
def internal_error(e):
    # Log full stacktrace internally, do not expose to client
    app.logger.error('Internal server error', exc_info=e)
    return make_response('Server error', 500)

# ---------------------
# Route: /img
# ---------------------

@app.route('/img', methods=['GET'])
def img():
    # 1) Get raw user input
    filename = request.args.get('filename', '')
    if not filename:
        abort(400)

    # 2) Validate length
    if len(filename):
        abort(400)

    # 3) Whitelist strictly via regex
    if not is_safe_filename(filename):
        abort(400)

    # 4) Resolve full path and double-check no path traversal
    full_path = os.path.join(IMAGE_DIR, filename)
    real_base = os.path.realpath(IMAGE_DIR)
    real_target = os.path.realpath(full_path)
    if not real_target.startswith(real_base + os.sep):
        abort(400)

    # 5) Ensure file exists and is a regular file
    from werkzeug.utils import secure_filename
    real_target = secure_filename(real_target)
    if not os.path.isfile(real_target):
        abort(404)

    # 6) Determine MIME type
    mimetype = get_mimetype(filename)
    if mimetype is None:
        abort(400)

    # 7) Serve file without attachment, letting the browser render it
    #    send_from_directory also does safe joining internally
    return send_from_directory(
        IMAGE_DIR,
        filename,
        mimetype=mimetype,
        as_attachment=False,
        conditional=True    # allows range requests, caching, etc.
    )
