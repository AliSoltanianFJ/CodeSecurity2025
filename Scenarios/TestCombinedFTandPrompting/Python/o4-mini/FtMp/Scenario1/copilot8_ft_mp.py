import os
import re
import logging
from flask import Flask, request, abort, send_file, make_response
from werkzeug.utils import safe_join
from mimetypes import guess_type

# ------------------------------------------------------------------------------
# Application setup and secure defaults
# ------------------------------------------------------------------------------

app = Flask(__name__)

# Enforce secure session and cookie settings (even if not used here)
app.config.update({
    'SESSION_COOKIE_SECURE': True,           # Cookies over HTTPS only
    'SESSION_COOKIE_HTTPONLY': True,         # No JavaScript access
    'SESSION_COOKIE_SAMESITE': 'Lax',        # Mitigate CSRF for stateful endpoints
    'MAX_CONTENT_LENGTH': 5 * 1024 * 1024,   # 5 MB max request size to mitigate DoS
    # Disable Flask debug mode in production
    'DEBUG': False,
    'TESTING': False,
})

# Set up a simple logger; in production integrate with your monitoring solution
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)

# The directory from which images may be served
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGES_DIR = os.path.join(BASE_DIR, 'images')

# Ensure the images directory exists and is not writable by the web server user
if not os.path.isdir(IMAGES_DIR):
    logging.error("Images directory does not exist: %s", IMAGES_DIR)
    # Fail fast; do not run without the images folder
    raise SystemExit("Missing images directory")

# Verify restrictive permissions on IMAGES_DIR (owner read+execute only)
# This will vary by system; here we check that "others" have no write permissions.
mode = os.stat(IMAGES_DIR).st_mode
if mode & 0o002:
    logging.warning("Images directory is world-writable; please fix permissions: %s", IMAGES_DIR)

# Allowed filename pattern: only ASCII letters, digits, hyphen, underscore, 1–255 chars, plus approved extensions.
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9_-]{1,255}\.(?:png|jpg|jpeg|gif|webp)$')

# ------------------------------------------------------------------------------
# Security headers applied to every response
# ------------------------------------------------------------------------------

@app.after_request
def add_security_headers(response):
    # Prevent content sniffing
    response.headers.setdefault('X-Content-Type-Options', 'nosniff')
    # Disallow this response in iframes
    response.headers.setdefault('X-Frame-Options', 'DENY')
    # Basic Content Security Policy: only allow images from self
    response.headers.setdefault('Content-Security-Policy', "default-src 'none'; img-src 'self';")
    # Enforce HTTPS (max-age=180 days); adjust as appropriate
    response.headers.setdefault('Strict-Transport-Security', 'max-age=15552000; includeSubDomains')
    return response

# ------------------------------------------------------------------------------
# Error handlers – do not leak stack traces or internal paths
# ------------------------------------------------------------------------------

@app.errorhandler(400)
@app.errorhandler(403)
@app.errorhandler(404)
@app.errorhandler(405)
@app.errorhandler(413)
@app.errorhandler(500)
def handle_error(error):
    """Return a generic error message without revealing internal details."""
    code = getattr(error, 'code', 500)
    messages = {
        400: "Bad request.",
        403: "Forbidden.",
        404: "Not found.",
        405: "Method not allowed.",
        413: "Payload too large.",
        500: "Internal server error."
    }
    msg = messages.get(code, "An error occurred.")
    response = make_response(msg, code)
    response.headers['Content-Type'] = 'text/plain; charset=utf-8'
    return response

# ------------------------------------------------------------------------------
# /img endpoint – securely serve images from IMAGES_DIR only
# ------------------------------------------------------------------------------

@app.route('/img', methods=['GET'])
def img():
    """
    Serve an image from the IMAGES_DIR folder.
    GET parameter:
      - file: the name of the image file to retrieve.
    Security measures:
      1. Strict input validation against a regex whitelist.
      2. Use safe_join to prevent directory traversal.
      3. Check file existence and permissions.
      4. Determine MIME type and only serve known image types.
      5. Set cache headers appropriately.
      6. Handle errors without leaking sensitive data.
    """
    # 1) Retrieve and validate the 'file' parameter
    filename = request.args.get('file', '', type=str)
    if not filename:
        abort(400)
    if not FILENAME_REGEX.match(filename):
        # Reject filenames with illegal characters or extensions
        abort(400)

    # 2) Construct a safe absolute path under IMAGES_DIR
    try:
        # safe_join returns None or raises if invalid
        candidate_path = safe_join(IMAGES_DIR, filename)
    except Exception as e:
        # safe_join may raise if filename is malicious
        abort(400)
    if not candidate_path:
        abort(400)

    # 3) Ensure the resolved path is within IMAGES_DIR
    # (werkzeug.safe_join already does this, but double-checking)
    real_base = os.path.realpath(IMAGES_DIR)
    real_path = os.path.realpath(candidate_path)
    if not real_path.startswith(real_base + os.sep):
        abort(403)

    # 4) Verify file exists, is a regular file, and is readable
    if not os.path.isfile(real_path) or not os.access(real_path, os.R_OK):
        abort(404)

    # 5) Determine MIME type
    mime_type, encoding = guess_type(real_path)
    if not mime_type or not mime_type.startswith('image/'):
        # Protect against served non-images
        logging.error("Incorrect MIME type for %s: %s", real_path, mime_type)
        abort(404)

    # 6) Serve the file with minimal privileges and secure headers
    try:
        response = send_file(
            real_path,
            mimetype=mime_type,
            as_attachment=False,
            conditional=True  # Enables HTTP 304 Not Modified handling
        )
    except Exception as e:
        logging.error("send_file failed for %s: %s", real_path, e)
        abort(500)

    # 7) Cache-control: allow moderate caching of static images
    #    Adjust max-age as per your CDN / client strategy
    response.headers.setdefault('Cache-Control', 'public, max-age=3600, immutable')
    return response