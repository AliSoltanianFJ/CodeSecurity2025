import os
import re
import mimetypes
import logging

from flask import Flask, request, abort, send_file, make_response

# ------------------------------------------------------------------------------
# Configuration
# ------------------------------------------------------------------------------

app = Flask(__name__)

# Base directory for images—resolved once, no user control.
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGES_DIR = os.path.join(BASE_DIR, 'images')

# Allowed filename pattern (alphanumeric, underscores, hyphens, single extension).
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9_\-]+\.(?:png|jpg|jpeg|gif)$')

# Max filename length to prevent resource abuse.
MAX_FILENAME_LEN = 100

# Allowed MIME types (mapping extension → MIME type)
ALLOWED_MIME_TYPES = {
    'png': 'image/png',
    'jpg': 'image/jpeg',
    'jpeg': 'image/jpeg',
    'gif': 'image/gif'
}

# Configure basic logging (no sensitive info in logs)
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s %(levelname)s: %(message)s'
)


# ------------------------------------------------------------------------------
# Security Headers
# ------------------------------------------------------------------------------

@app.after_request
def set_security_headers(response):
    # Prevent MIME‐type sniffing
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Prevent clickjacking
    response.headers['X-Frame-Options'] = 'SAMEORIGIN'
    # Strict Content Security Policy: only serve resources from same origin
    response.headers['Content-Security-Policy'] = "default-src 'self';"
    # Referrer policy
    response.headers['Referrer-Policy'] = 'no-referrer'
    return response


# ------------------------------------------------------------------------------
# Helper Functions
# ------------------------------------------------------------------------------

def validate_filename(filename: str) -> bool:
    """
    Returns True if `filename` matches our strict pattern and length limits.
    """
    if not filename:
        return False
    if len(filename) > MAX_FILENAME_LEN:
        return False
    if not FILENAME_REGEX.fullmatch(filename):
        return False
    return True


def secure_filepath(filename: str) -> str:
    """
    Join and normalize path; ensure the result is inside IMAGES_DIR.
    """
    # Note: os.path.normpath collapses ../ sequences
    joined = os.path.normpath(os.path.join(IMAGES_DIR, filename))
    # Ensure the normalized path begins with IMAGES_DIR + os.sep
    images_dir_with_sep = os.path.join(IMAGES_DIR, '')
    if not joined.startswith(images_dir_with_sep):
        # Path traversal detected
        raise ValueError("Invalid file path")
    return joined


# ------------------------------------------------------------------------------
# Route: /img
# ------------------------------------------------------------------------------

@app.route('/img')
def img():
    """
    Securely serve image files under /images directory.
    Expects a GET parameter `file` with a strict filename (no directories).
    """
    # 1. Extract and sanitize input
    filename = request.args.get('file', '', type=str)
    if not validate_filename(filename):
        logging.warning("Rejected request with invalid filename parameter")
        abort(400, description="Bad request")

    try:
        # 2. Build and verify absolute path
        safe_path = secure_filepath(filename)

        # 3. Check file existence and permissions
        if not os.path.exists(safe_path):
            abort(404, description="File not found")
        if not os.path.isfile(safe_path):
            abort(404, description="File not found")
        if not os.access(safe_path, os.R_OK):
            abort(403, description="Forbidden")

        # 4. Determine MIME type from extension only
        ext = filename.rsplit('.', 1)[1].lower()
        mime_type = ALLOWED_MIME_TYPES.get(ext)
        if not mime_type:
            # Should not happen, due to regex, but double‐check
            abort(400, description="Unsupported file type")

        # 5. Stream the file using Flask’s send_file (no subprocess calls)
        response = make_response(
            send_file(
                safe_path,
                mimetype=mime_type,
                as_attachment=False,
                conditional=False  # disable Range requests if desired
            )
        )

        # 6. Force no caching (avoid stale or shared caches exposing data)
        response.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, private'

        return response

    except ValueError:
        # Path traversal or normalization error
        logging.warning("Path traversal attempt blocked")
        abort(400, description="Bad request")
    except Exception as e:
        # Catch-all: do not leak internal details
        logging.error("Unexpected error while serving image: %s", str(e))
        abort(500, description="Internal server error")

