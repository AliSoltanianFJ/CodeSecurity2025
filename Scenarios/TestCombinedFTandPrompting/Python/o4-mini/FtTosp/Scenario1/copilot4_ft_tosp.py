import os
import re
from flask import Flask, request, send_file, abort, make_response

# -----------------------------------------------------------------------------
# Configuration
# -----------------------------------------------------------------------------
app = Flask(__name__)

# Ensure no debug info or stack traces are exposed
app.config.update(
    DEBUG=False,
    PROPAGATE_EXCEPTIONS=False,
)

# Whitelisted file extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'webp'}

# Precompile a regex that only permits letters, digits, underscore, hyphen and dot
FILENAME_PATTERN = re.compile(r'^[A-Za-z0-9._-]+$')

# Absolute path to the images folder
BASE_DIR = os.path.abspath(os.path.dirname(__file__))
IMAGES_DIR = os.path.join(BASE_DIR, 'images')


# -----------------------------------------------------------------------------
# Utility Functions
# -----------------------------------------------------------------------------
def is_valid_filename(filename: str) -> bool:
    """
    Check that the filename:
     - is not empty
     - is under reasonable length
     - matches our safe pattern
     - has a single dot and an allowed extension
    """
    if not filename or len(filename) > 100:
        return False

    if not FILENAME_PATTERN.fullmatch(filename):
        return False

    parts = filename.rsplit('.', 1)
    if len(parts) != 2:
        return False

    ext = parts[1].lower()
    return ext in ALLOWED_EXTENSIONS


def resolve_image_path(filename: str) -> str:
    """
    Construct an absolute path to the requested file and verify
    that it lives under IMAGES_DIR to prevent directory traversal.
    Returns the absolute path if valid, otherwise None.
    """
    candidate = os.path.abspath(os.path.join(IMAGES_DIR, filename))
    # Ensure the requested path is within our images directory
    if not candidate.startswith(IMAGES_DIR + os.sep):
        return None
    if not os.path.isfile(candidate):
        return None
    return candidate


def extension_to_mime(ext: str) -> str:
    """
    Map a file extension to the correct image MIME type.
    """
    mapping = {
        'png':  'image/png',
        'jpg':  'image/jpeg',
        'jpeg': 'image/jpeg',
        'gif':  'image/gif',
        'webp': 'image/webp'
    }
    return mapping.get(ext.lower(), 'application/octet-stream')


# -----------------------------------------------------------------------------
# Routes
# -----------------------------------------------------------------------------
@app.route('/img')
def img():
    # 1) Extract and sanitize input
    filename = request.args.get('file', '', type=str).strip()

    if not is_valid_filename(filename):
        # Invalid input: reject without details
        abort(400)

    # 2) Resolve and verify path
    image_path = resolve_image_path(filename)
    if image_path is None:
        # Either traversal detected or file missing
        abort(404)

    # 3) Determine MIME type from extension
    ext = filename.rsplit('.', 1)[1].lower()
    mime_type = extension_to_mime(ext)

    # 4) Serve file securely with streaming and conditional requests
    #    Flask will handle byte ranges and streaming efficiently.
    try:
        return send_file(
            image_path,
            mimetype=mime_type,
            as_attachment=False,
            conditional=True
        )
    except Exception:
        # Any unexpected failure: do not leak internal state
        abort(500)


# -----------------------------------------------------------------------------
# Error Handlers
# -----------------------------------------------------------------------------
# We return empty bodies with the appropriate status code to avoid XSS or info leakage.
@app.errorhandler(400)
def bad_request(error=None):
    return make_response(('', 400))

@app.errorhandler(404)
def not_found(error=None):
    return make_response(('', 404))

@app.errorhandler(500)
def server_error(error=None):
    return make_response(('', 500))


# -----------------------------------------------------------------------------
# After-Request Security Headers
# -----------------------------------------------------------------------------
@app.after_request
def set_security_headers(response):
    # Prevent MIME-type sniffing
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Disallow framing to thwart clickjacking
    response.headers['X-Frame-Options'] = 'DENY'
    # Restrict sources: only images from self
    response.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self';"
    # Optionally, set a strict referrer policy
    response.headers['Referrer-Policy'] = 'no-referrer'
    return response

