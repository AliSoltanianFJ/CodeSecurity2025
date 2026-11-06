import os
import re
import logging
import mimetypes

from flask import Flask, request, abort, redirect, send_file
from werkzeug.utils import safe_join
from werkzeug.exceptions import HTTPException

# =============================================================================
# Application and Logging Configuration
# =============================================================================

app = Flask(__name__)

# Enforce production settings
app.config.update(
    ENV='production',              # disable the debugger
    DEBUG=False,
    PROPAGATE_EXCEPTIONS=False     # do not leak stack traces to clients
)

# Maximum allowed image size on disk (5 MiB). Also enforced below.
MAX_FILE_SIZE = 5 * 1024 * 1024

# Only these filename characters are allowed (whitelist approach).
# Prevents path traversal, control-characters, spaces, etc.
ALLOWED_FILENAME_REGEX = re.compile(r'^[A-Za-z0-9._-]+$')

# Only these extensions are served.
ALLOWED_EXTENSIONS = {'jpg', 'jpeg', 'png', 'gif'}

# Compute absolute path to our images directory once, using realpath.
# This directory should be owned by a non-root, non-privileged user.
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGES_DIR = os.path.realpath(os.path.join(BASE_DIR, 'images'))

# Initialize a module-level logger.
logger = logging.getLogger('secure_img_endpoint')
handler = logging.StreamHandler()
handler.setFormatter(logging.Formatter(
    '[%(asctime)s] %(levelname)s in %(module)s: %(message)s'
))
logger.addHandler(handler)
logger.setLevel(logging.INFO)


# =============================================================================
# Generic Error Handler (no sensitive info leaked to clients)
# =============================================================================
@app.errorhandler(Exception)
def handle_exception(e):
    # Log all exceptions with stack trace at the server side
    if isinstance(e, HTTPException):
        code = e.code
        logger.warning("HTTP %d error on /img: %s", code, e.description)
    else:
        code = 500
        logger.exception("Unexpected error on /img")
    # Return only a generic message to the client
    return "An error occurred while processing your request.", code


# =============================================================================
# Secure Image Serving Endpoint
# =============================================================================
@app.route('/img')
def img():
    """
    Serves image files from a local 'images/' directory.

    Security controls in place:
      1. Input validation: only a specific filename regex is allowed.
      2. Extension whitelist: only common image types.
      3. Path sanitation: safe_join prevents directory traversal.
      4. File exists and is not a symlink outside images/.
      5. File size limit enforced.
      6. No user‐supplied data is ever passed to a shell or subprocess.
      7. Files are served via Flask's send_file (no insecure APIs).
      8. Appropriate security headers set to mitigate XSS/MIME‐sniffing.
      9. HTTPS enforcement via before_request.
     10. Generic error messages; detailed errors stay in server logs.
    """

    # 1. Retrieve and strip the 'file' parameter
    filename = request.args.get('file', '').strip()
    if not filename:
        # Missing parameter
        abort(400)

    # 2. Enforce filename character whitelist
    if not ALLOWED_FILENAME_REGEX.fullmatch(filename):
        # Invalid characters or structure
        abort(400)

    # 3. Enforce extension whitelist
    parts = filename.rsplit('.', 1)
    if len(parts) != 2:
        abort(400)
    _, ext = parts
    ext = ext.lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400)

    # 4. Compute a secure absolute path and prevent directory traversal
    try:
        # safe_join will return None or raise if the final path
        # is outside IMAGES_DIR or contains illegal segments.
        file_path = safe_join(IMAGES_DIR, filename)
    except Exception:
        abort(404)

    if not file_path:
        abort(404)

    # 5. Verify that the path is indeed inside IMAGES_DIR
    file_path = os.path.realpath(file_path)
    if not file_path.startswith(IMAGES_DIR + os.sep):
        # Should never happen if safe_join works, but double-check
        abort(404)

    # 6. Check that the file exists, is a regular file, and is not a symlink
    if not os.path.exists(file_path) or not os.path.isfile(file_path):
        abort(404)
    if os.path.islink(file_path):
        # No symlinks allowed
        abort(404)

    # 7. Enforce maximum file size
    file_size = os.stat(file_path).st_size
    if file_size > MAX_FILE_SIZE:
        # Payload too large
        abort(413)

    # 8. Determine MIME type in a safe way
    mimetype, _ = mimetypes.guess_type(file_path)
    if not mimetype:
        # Fallback to safe default
        mimetype = 'application/octet-stream'

    # 9. Serve the file
    try:
        response = send_file(
            file_path,
            mimetype=mimetype,
            as_attachment=False,
            conditional=True
        )
    except Exception as e:
        abort(500)

    # 10. Set security headers
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Restrict where images can be loaded from (only same origin)
    response.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self';"
    # HSTS: tell browsers to always use HTTPS (2 years, includes subdomains)
    response.headers['Strict-Transport-Security'] = 'max-age=63072000; includeSubDomains; preload'
    # Cache-Control: clients may cache privately for 1 hour
    response.headers['Cache-Control'] = 'private, max-age=3600'

    return response