import os
import logging
import imghdr
from flask import Flask, request, abort, send_file, redirect, current_app
from werkzeug.utils import secure_filename
from werkzeug.middleware.proxy_fix import ProxyFix

# -----------------------------------------------------------------------------
# Configuration and Initialization
# -----------------------------------------------------------------------------

app = Flask(__name__)

# Security-sensitive defaults
app.config['DEBUG'] = False                          # No debug traces leaked
app.config['PROPAGATE_EXCEPTIONS'] = False            # Do not propagate exceptions
app.config['ENFORCE_SSL'] = True                      # Redirect HTTP→HTTPS
app.config['IMAGE_DIR'] = os.getenv('IMAGE_DIR', '/var/app/images')
app.config['ALLOWED_EXTENSIONS'] = {'png', 'jpg', 'jpeg', 'gif', 'webp'}
app.config['MAX_IMAGE_SIZE'] = 5 * 1024 * 1024        # 5 MB max per image

# Ensure IMAGE_DIR is absolute and normalized
app.config['IMAGE_DIR'] = os.path.realpath(app.config['IMAGE_DIR'])

# Set up logging: warnings and errors only
handler = logging.StreamHandler()
handler.setLevel(logging.WARNING)
formatter = logging.Formatter(
    '%(asctime)s %(levelname)s %(name)s: %(message)s'
)
handler.setFormatter(formatter)
app.logger.addHandler(handler)
app.logger.setLevel(logging.WARNING)

# If behind a proxy/load-balancer, honor X-Forwarded-* for secure detection
app.wsgi_app = ProxyFix(app.wsgi_app, x_proto=1, x_host=1)

# -----------------------------------------------------------------------------
# Error Handlers (no sensitive info leaked)
# -----------------------------------------------------------------------------

@app.errorhandler(400)
def bad_request(e):
    return 'Bad request', 400

@app.errorhandler(403)
def forbidden(e):
    return 'Forbidden', 403

@app.errorhandler(404)
def not_found(e):
    return 'Not found', 404

@app.errorhandler(413)
def file_too_large(e):
    return 'File too large', 413

@app.errorhandler(Exception)
def internal_error(e):
    app.logger.error('Internal error: %s', e)
    return 'Internal server error', 500

# -----------------------------------------------------------------------------
# Helper Functions
# -----------------------------------------------------------------------------

def is_allowed_extension(filename):
    """
    Check that the extension is in the whitelist.
    OWASP A9: Using only safe file types.
    """
    if '.' not in filename:
        return False
    ext = filename.rsplit('.', 1)[1].lower()
    return ext in current_app.config['ALLOWED_EXTENSIONS']

def safe_image_path(filename):
    """
    Build a secure, absolute path to the image.
    Prevents directory traversal by:
      - Sanitizing the filename
      - Joining with IMAGE_DIR
      - Verifying the realpath is within IMAGE_DIR
    """
    # 1. Sanitize filename
    secure_name = secure_filename(filename)
    # Reject if filename changed or empty
    if not secure_name or secure_name != filename:
        abort(400)

    # 2. Check allowed extension
    if not is_allowed_extension(secure_name):
        app.logger.warning('Disallowed file extension: %r', secure_name)
        abort(400)

    # 3. Build absolute path
    abs_path = os.path.realpath(os.path.join(current_app.config['IMAGE_DIR'], secure_name))

    # 4. Enforce IMAGE_DIR boundary
    image_dir = current_app.config['IMAGE_DIR']
    if not abs_path.startswith(image_dir + os.sep):
        abort(403)

    # 5. Verify existence and size
    try:
        stat = os.stat(abs_path)
    except OSError:
        app.logger.info('File not found: %r', abs_path)
        abort(404)
    if not os.path.isfile(abs_path):
        app.logger.warning('Not a file: %r', abs_path)
        abort(404)
    if stat.st_size > current_app.config['MAX_IMAGE_SIZE']:
        app.logger.warning('File too large: %r (%d bytes)', abs_path, stat.st_size)
        abort(413)

    return abs_path

# -----------------------------------------------------------------------------
# Route Definitions
# -----------------------------------------------------------------------------

@app.route('/img')
def img():
    """
    Serve images from a safe directory.
    GET parameter: filename
    Example: GET /img?filename=picture.png
    """

    # 1. Retrieve and validate input
    filename = request.args.get('filename', '', type=str)
    if not filename:
        app.logger.warning('No filename provided')
        abort(400)

    # 2. Resolve and secure the path
    image_path = safe_image_path(filename)

    # 3. Verify actual file content is an image
    image_type = imghdr.what(image_path)
    if image_type not in current_app.config['ALLOWED_EXTENSIONS']:
        app.logger.warning('File content mismatch or unsupported image type: %r -> %r',
                           image_path, image_type)
        abort(403)

    # 4. Serve the file with proper headers
    # Prevent MIME sniffing (XSS protection), instruct no caching of sensitive images
    response = send_file(
        image_path,
        mimetype=f'image/{image_type}',
        as_attachment=False,
        conditional=True  # allow HTTP 304 handling
    )
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # We can adjust caching policies as needed. Example: cache for 1 hour
    response.headers['Cache-Control'] = 'public, max-age=3600, no-transform'

    return response