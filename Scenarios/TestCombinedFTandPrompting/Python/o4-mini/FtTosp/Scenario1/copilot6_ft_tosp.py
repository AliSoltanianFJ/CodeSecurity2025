from flask import Flask, request, send_file, abort, make_response
from werkzeug.utils import secure_filename
import os
import re

app = Flask(__name__)

# Disable debug to avoid leaking stack traces
app.config['DEBUG'] = False

# Base directory for images (absolute path)
APP_ROOT    = os.path.dirname(os.path.abspath(__file__))
IMAGES_DIR  = os.path.join(APP_ROOT, 'images')

# Allowed file extensions and filename pattern
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}
FILENAME_REGEX     = re.compile(r'^[A-Za-z0-9_\-]+\.(?:png|jpg|jpeg|gif)$')

# Maximum filename length to prevent excessively long input
MAX_FILENAME_LENGTH = 100

def is_allowed_filename(filename):
    """
    Returns True if filename matches the safe pattern and has an allowed extension.
    """
    return bool(FILENAME_REGEX.fullmatch(filename))

@app.errorhandler(400)
@app.errorhandler(404)
@app.errorhandler(500)
def handle_error(error):
    """
    Generic error handler that never leaks internal state.
    """
    code = error.code if hasattr(error, 'code') else 500
    # Return a plain-text message
    resp = make_response('An error occurred', code)
    resp.headers['Content-Type'] = 'text/plain; charset=utf-8'
    return resp

@app.after_request
def set_security_headers(response):
    """
    Apply HTTP security headers to every response.
    """
    # Prevent MIME‐type sniffing
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Disallow loading any resources except images from self
    response.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self';"
    # Cache images privately for up to 1 hour
    response.headers['Cache-Control'] = 'private, max-age=3600'
    return response

@app.route('/img')
def img():
    """
    Securely serve an image from the local images directory.
    GET parameter:
      - file: the image filename to retrieve
    """
    # 1. Retrieve and preliminarily validate input
    filename = request.args.get('file', '').strip()
    if not filename:
        # Missing parameter
        abort(400)

    # 2. Enforce filename length limit
    if len(filename) > MAX_FILENAME_LENGTH:
        abort(400)

    # 3. Must match strict pattern: alphanumerics, underscore, hyphen + allowed extension
    if not is_allowed_filename(filename):
        abort(400)

    # 4. Further sanitize via werkzeug to remove any odd paths
    safe_name = secure_filename(filename)
    if safe_name != filename:
        # secure_filename altered it—reject
        abort(400)

    # 5. Construct absolute path and enforce directory confinement
    image_path = os.path.join(IMAGES_DIR, safe_name)
    try:
        real_base  = os.path.realpath(IMAGES_DIR) + os.sep
        real_target = os.path.realpath(image_path)
    except (OSError, ValueError):
        abort(400)

    # Reject any attempt to break out of IMAGES_DIR
    if not real_target.startswith(real_base):
        abort(400)

    # 6. Ensure file exists and is a regular file
    if not os.path.isfile(real_target):
        abort(404)

    # 7. Serve the file using Flask’s safe helper
    try:
        # Flask will set correct Content-Type based on file extension
        return send_file(real_target, as_attachment=False)
    except Exception:
        # Any unexpected error
        abort(500)
