import os
from flask import Flask, request, send_from_directory, abort, current_app
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Configuration
# 1) Absolute path to your images directory
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGES_DIR = os.path.join(BASE_DIR, 'images')

# 2) Whitelisted extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}

# 3) Maximum filename length to avoid resource exhaustion
MAX_FILENAME_LENGTH = 255

# Ensure the images directory exists and is not a symlink to elsewhere
if not os.path.isdir(IMAGES_DIR) or os.path.islink(IMAGES_DIR):
    raise RuntimeError("Images directory is missing or insecure.")

def is_allowed_filename(filename: str) -> bool:
    """
    Check that the filename has an allowed extension and is reasonably sized.
    """
    if not filename:
        return False

    if len(filename) > MAX_FILENAME_LENGTH:
        return False

    name_part, dot, ext = filename.rpartition('.')
    if not dot:
        return False

    # Lower-case extension check
    if ext.lower() not in ALLOWED_EXTENSIONS:
        return False

    return True

@app.route('/img', methods=['GET'])
def img():
    """
    Serve an image file from the images directory, given a ?file=<filename> query parameter.
    Security measures:
      - Input is strictly validated and sanitized.
      - Directory traversal is prevented by secure_filename and realpath checks.
      - Only whitelisted extensions are allowed.
      - No traceback or internal information is leaked on errors.
      - Caching is disabled to avoid stale content issues.
    """
    # 1) Fetch raw user input
    raw_filename = request.args.get('file', '')

    # 2) Quick reject on missing or oversized input
    if not raw_filename or len(raw_filename) > MAX_FILENAME_LENGTH:
        abort(400)  # Bad Request

    # 3) Sanitize the filename (this strips path separators, control chars, etc.)
    safe_filename = secure_filename(raw_filename)

    # 4) Ensure the sanitized filename matches exactly the user’s (reject attempts to bypass)
    if safe_filename != raw_filename:
        abort(400)

    # 5) Ensure extension is allowed
    if not is_allowed_filename(safe_filename):
        abort(400)

    # 6) Construct absolute path and prevent directory traversal
    #    os.path.commonpath will raise if paths are on different drives
    try:
        requested_path = os.path.abspath(os.path.join(IMAGES_DIR, safe_filename))
    except Exception:
        abort(400)

    if os.path.commonpath([IMAGES_DIR, requested_path]) != IMAGES_DIR:
        abort(400)

    # 7) Check file existence and that it's a regular file
    if not os.path.isfile(requested_path):
        abort(404)  # Not Found

    # 8) Serve the file without forcing download; let the browser render it if possible
    response = send_from_directory(
        directory=IMAGES_DIR,
        filename=safe_filename,
        as_attachment=False,
        conditional=True  # enables HTTP 304 handling
    )

    # 9) Additional headers to harden caching/policies
    response.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, max-age=0'
    response.headers['Pragma'] = 'no-cache'
    response.headers['X-Content-Type-Options'] = 'nosniff'

    return response

# Generic error handlers to avoid leaking internal info
@app.errorhandler(400)
def bad_request(_):
    return ("Bad request.", 400)

@app.errorhandler(404)
def not_found(_):
    return ("Not found.", 404)

@app.errorhandler(500)
def internal_error(_):
    return ("An internal error occurred.", 500)
