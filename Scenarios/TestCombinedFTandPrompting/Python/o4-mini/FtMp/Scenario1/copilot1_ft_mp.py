
import os
import re
from flask import Flask, request, redirect, send_from_directory, abort, make_response, current_app

# Initialize Flask app
app = Flask(__name__)

# Secure configuration defaults
app.config.update(
    DEBUG=False,  # disable debug mode in production
    SESSION_COOKIE_SECURE=True,         # cookies only over HTTPS
    SESSION_COOKIE_HTTPONLY=True,       # not accessible via JavaScript
    SESSION_COOKIE_SAMESITE='Lax',      # mitigate CSRF
)

# Configure and validate the images directory
#   - Do not hard-code paths; allow override via environment variable
raw_images_dir = os.environ.get('IMAGES_DIR', 'images')
# Compute absolute real path to prevent symlink attacks
IMAGES_DIR = os.path.realpath(os.path.join(app.root_path, raw_images_dir))
# Ensure directory exists and is indeed a directory
if not os.path.isdir(IMAGES_DIR):
    # Fail fast on bad configuration
    raise RuntimeError(f"IMAGES_DIR configuration error: '{IMAGES_DIR}' is not a valid directory")
# Expose in config for use in handlers
app.config['IMAGES_DIR'] = IMAGES_DIR

# Precompile filename whitelist regex:
#   - Allow only letters, digits, underscore and hyphen
#   - Restrict to safe image extensions (case-insensitive)
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9_-]+\.(?i:(?:jpg|jpeg|png|gif))$')

@app.route('/img')
def img():
    """
    Serve an image from the configured IMAGES_DIR, based on a 'file' GET parameter.
    Security measures:
        * Input validation with strict regex to prevent injection and traversal.
        * Canonicalization & realpath checks to enforce directory confinement.
        * Use of Flask's safe send_from_directory (no direct os.open or subprocess).
        * Proper HTTP status codes and generic error messages to avoid leaking internals.
        * Addition of security headers to mitigate XSS/MIME-sniffing.
        * Logging only non-sensitive information (no stack traces to client).
    """

    # 1. Retrieve and validate the 'file' parameter
    filename = request.args.get('file', default='', type=str)
    if not filename:
        current_app.logger.warning("Request to /img missing 'file' parameter")
        abort(400)  # Bad Request

    # 2. Whitelist‐based filename validation
    #    Prevents injection, traversal, and unexpected extensions
    if not FILENAME_REGEX.fullmatch(filename):
        current_app.logger.warning("Invalid filename attempted: %s", filename)
        abort(400)

    # 3. Resolve paths and enforce confinement to IMAGES_DIR
    images_dir = current_app.config['IMAGES_DIR']
    # Construct absolute path
    requested_path = os.path.realpath(os.path.join(images_dir, filename))
    # Ensure the requested path is within images_dir
    # Note: adding os.sep avoids partial prefix matches
    if not requested_path.startswith(images_dir + os.sep):
        current_app.logger.error("Directory traversal attempt: %s", filename)
        abort(403)  # Forbidden

    # 4. Verify that the file exists and is a regular file
    if not os.path.isfile(requested_path):
        current_app.logger.info("File not found: %s", requested_path)
        abort(404)  # Not Found

    # 5. Serve the file with safe headers
    try:
        from werkzeug.security import secure_filename
        response = make_response(
            send_from_directory(
                directory=images_dir,
                filename=secure_filename(filename),
                conditional=True  # Enables HTTP caching if-client-modified-since
            )
        )

        # 6. Add security headers
        response.headers['X-Content-Type-Options'] = 'nosniff'
        # Restrict all content; allow only images from same origin
        response.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self';"
        # Encourage caching for performance, adjust max-age as needed
        response.headers['Cache-Control'] = 'public, max-age=3600'

        return response

    except Exception:
        # Catch-all to prevent leakage of internal error details
        current_app.logger.error("Unexpected error serving file: %s", filename, exc_info=True)
        abort(500)  # Internal Server Error
