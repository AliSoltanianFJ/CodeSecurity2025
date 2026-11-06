import os
import re
import logging
from flask import Flask, request, send_from_directory, abort, current_app, make_response
from werkzeug.utils import secure_filename
from flask_talisman import Talisman

# ------------------------------------------------------------------------------
# App setup
# ------------------------------------------------------------------------------

app = Flask(__name__)

# Enforce HTTPS, HSTS, and sane security headers via flask-talisman
# (Requires: pip install flask-talisman)
Talisman(
    app,
    content_security_policy={
        # Adjust CSP as appropriate for your app’s broader needs
        'default-src': ["'self'"],
        'img-src': ["'self'"],
        'style-src': ["'self'"],
        'script-src': ["'self'"]
    },
    force_https=True,
    strict_transport_security=True,
    strict_transport_security_max_age=31536000,  # 1 year
    strict_transport_security_include_subdomains=True
)

# Disable development features in production
app.config['ENV'] = 'production'
app.config['DEBUG'] = False

# Maximum file size served (to avoid DoS via very large files)
app.config['MAX_CONTENT_LENGTH'] = 4 * 1024 * 1024  # 4 MB

# Directory where images are stored; must be owned by a least-privileged user
# and not writable by the web server process.
IMAGES_DIR = os.path.join(app.root_path, 'images')

# Only allow these file extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

# Precompile a regex for extra safety (restricts filenames to alphanumerics, dashes, underscores)
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9_-]+\.(?:png|jpg|jpeg|gif)$')

# Set up logging with a rotating file handler (adjust path and permissions as needed)
handler = logging.handlers.RotatingFileHandler(
    filename=os.path.join(app.root_path, 'logs', 'app.log'),
    maxBytes=5 * 1024 * 1024,
    backupCount=3,
    encoding='utf-8'
)
handler.setLevel(logging.INFO)
formatter = logging.Formatter(
    '%(asctime)s %(levelname)s [%(remote_addr)s] %(message)s'
)
handler.setFormatter(formatter)
app.logger.addHandler(handler)

# ------------------------------------------------------------------------------
# Error handlers – do not leak internal details
# ------------------------------------------------------------------------------

def log_error(exc):
    # Log the stack trace at ERROR level
    app.logger.error('Unhandled exception')

@app.errorhandler(400)
def bad_request(e):
    return make_response('Bad request.', 400)

@app.errorhandler(404)
def not_found(e):
    return make_response('Not found.', 404)

@app.errorhandler(500)
def internal_error(e):
    return make_response('Internal server error.', 500)

# ------------------------------------------------------------------------------
# Helper functions
# ------------------------------------------------------------------------------

def is_safe_filename(filename: str) -> bool:
    """
    Validates that the filename:
      - Matches our strict regex (no path separators, only allowed chars).
      - Has an allowed extension.
    """
    return bool(FILENAME_REGEX.fullmatch(filename))

# ------------------------------------------------------------------------------
# Image endpoint
# ------------------------------------------------------------------------------

@app.route('/img', methods=['GET'])
def img():
    """
    Securely serve images from the IMAGES_DIR folder.
    GET parameter: ?file=<filename>
    """
    try:
        # 1) Retrieve and validate input
        filename = request.args.get('file', type=str, default='').strip()
        if not filename:
            abort(400)

        # 2) Reject unsafe filenames early
        if not is_safe_filename(filename):
            abort(400)

        # 3) Further secure the filename (removes any stray path info)
        filename = secure_filename(filename)

        # 4) Construct the absolute path and ensure it stays within IMAGES_DIR
        # send_from_directory performs its own check, but we double-check
        full_path = os.path.join(IMAGES_DIR, filename)
        if not full_path.startswith(os.path.abspath(IMAGES_DIR) + os.sep):
            # Directory traversal attempt
            abort(400)

        # 5) Ensure the file exists, is a regular file, and is owned by expected user
        if not os.path.isfile(full_path):
            abort(404)

        # 6) Serve the file with safe headers and no attachment
        #    Cache for up to 1 hour (adjust as needed)
        response = send_from_directory(
            directory=IMAGES_DIR,
            filename=filename,
            as_attachment=False,
            mimetype=None,        # Let Flask/werkzeug infer from extension
            cache_timeout=3600
        )

        # 7) Add extra headers to prevent MIME sniffing and clickjacking
        response.headers['X-Content-Type-Options'] = 'nosniff'
        response.headers['X-Frame-Options'] = 'DENY'

        return response

    except werkzeug.exceptions.HTTPException as http_exc:
        # If it’s an HTTPException (e.g. abort(404)), just re-raise to invoke our error handler
        raise http_exc

    except Exception as exc:
        # 8) Log unexpected errors without revealing details to the client
        log_error(exc)
        abort(500)