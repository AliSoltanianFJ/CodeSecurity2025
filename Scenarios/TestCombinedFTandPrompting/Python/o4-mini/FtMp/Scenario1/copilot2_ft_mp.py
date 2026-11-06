import os
import re
import logging
from flask import Flask, request, send_from_directory, abort
from flask_talisman import Talisman

# -- Configuration and Initialization --

app = Flask(__name__, static_folder=None)  # disable default static file handler
# Enforce HTTPS, HSTS, secure headers (CSP, X-Frame-Options, etc.)
Talisman(app,
         force_https=True,
         strict_transport_security=True,
         strict_transport_security_max_age=31536000,  # one year
         content_security_policy={
             'default-src': ["'none'"],
             'img-src': ["'self'"]
         })

# Limit maximum response body size (protect against DoS via huge files)
app.config['MAX_CONTENT_LENGTH'] = 5 * 1024 * 1024  # 5 MiB

# Application logger (no traceback to clients)
handler = logging.FileHandler('app.log', encoding='utf-8')
handler.setFormatter(logging.Formatter(
    '%(asctime)s [%(levelname)s] %(remote_addr)s %(message)s'
))
handler.setLevel(logging.INFO)
app.logger.addHandler(handler)
app.logger.setLevel(logging.INFO)

# Base directory for images (absolute, no symlinks)
BASE_DIR = os.path.abspath(os.path.dirname(__file__))
IMAGE_DIR = os.path.join(BASE_DIR, 'images')

# Ensure IMAGE_DIR exists and is not writable by the application user
if not os.path.isdir(IMAGE_DIR):
    raise RuntimeError('Image directory does not exist or is not accessible')

# Whitelist of allowed filename pattern (prevents path traversal and injection)
# Only alphanumeric, underscore, hyphen, followed by a valid extension
FILENAME_RE = re.compile(r'^[A-Za-z0-9_-]+\.(?:png|jpe?g|gif|webp)$')

# Mapping from extension to MIME type
MIME_TYPES = {
    'png':  'image/png',
    'jpg':  'image/jpeg',
    'jpeg': 'image/jpeg',
    'gif':  'image/gif',
    'webp': 'image/webp'
}


# -- Utility Functions --

def log_request(level, message, **kwargs):
    """
    Centralized logging. We inject client IP for each log entry.
    """
    extra = {'remote_addr': request.remote_addr}
    extra.update(kwargs)
    print(level, message, extra=extra)


def is_valid_image_header(path):
    """
    Perform a minimal magic‐number check on the first bytes of the file
    to ensure it matches its extension. Prevents simple SSRF/RCE attempts
    via specially crafted files.
    """
    try:
        with open(path, 'rb') as f:
            header = f.read(12)
    except OSError:
        return False

    # PNG
    if header.startswith(b'\x89PNG\r\n\x1a\n'):
        return True
    # JPEG
    if header.startswith(b'\xff\xd8\xff'):
        return True
    # GIF87a or GIF89a
    if header.startswith(b'GIF87a') or header.startswith(b'GIF89a'):
        return True
    # WebP (RIFF....WEBP)
    if header[:4] == b'RIFF' and header[8:12] == b'WEBP':
        return True

    return False


def secure_join(base, *paths):
    """
    Safely join one or more path components to `base` and ensure
    the result is still within the `base` directory.
    """
    candidate = os.path.abspath(os.path.join(base, *paths))
    if os.path.commonpath([base, candidate]) != base:
        raise ValueError('Attempted Path Traversal')
    return candidate


# -- Custom Error Handlers (no sensitive info leaked) --

@app.errorhandler(400)
def handle_bad_request(e):
    return 'Bad request', 400

@app.errorhandler(404)
def handle_not_found(e):
    return 'Not found', 404

@app.errorhandler(413)
def handle_request_too_large(e):
    return 'File too large', 413

@app.errorhandler(500)
def handle_server_error(e):
    return 'Internal server error', 500


# -- Image Serving Endpoint --

@app.route('/img', methods=['GET'])
def img():
    """
    Securely serve an image from the local 'images' directory.
    Query parameter:
      ?file=<filename>
    where <filename> must match our whitelist regex and reside under IMAGE_DIR.
    """

    # 1. Retrieve and normalize input
    filename = request.args.get('file', '').strip()
    if not filename:
        log_request(logging.WARNING, 'Missing "file" parameter')
        abort(400)

    # 2. Validate filename against whitelist regex
    if not FILENAME_RE.fullmatch(filename):
        log_request(logging.WARNING, 'Invalid filename pattern: %s', filename)
        abort(400)

    # 3. Construct filesystem path safely and prevent traversal
    try:
        full_path = secure_join(IMAGE_DIR, filename)
    except ValueError:
        log_request(logging.WARNING, 'Path traversal attempt: %s', filename)
        abort(400)

    # 4. Ensure the file exists, is a file (not a directory), and is non-executable
    if not os.path.isfile(full_path):
        log_request(logging.INFO, 'File not found: %s', filename)
        abort(404)
    if os.access(full_path, os.X_OK):
        # Deny execution of any served file
        log_request(logging.WARNING, 'Executable file block: %s', filename)
        abort(404)

    # 5. Perform a basic magic‐number check to verify the extension matches content
    if not is_valid_image_header(full_path):
        log_request(logging.WARNING, 'Invalid image header: %s', filename)
        abort(400)

    # 6. Serve the file with the correct MIME type, inline (not attachment)
    ext = filename.rsplit('.', 1)[1].lower()
    mimetype = MIME_TYPES.get(ext, 'application/octet-stream')

    try:
        response = send_from_directory(IMAGE_DIR,
                                       filename,
                                       as_attachment=False,
                                       mimetype=mimetype,
                                       conditional=True)
    except Exception as e:
        log_request(logging.ERROR, 'Error sending file %s: %s', filename, str(e))
        abort(500)

    # 7. Additional response hardening headers (reinforced by Talisman above)
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['Cache-Control'] = 'public, max-age=86400'  # one day

    return response


# -- Application Entry Point --

if __name__ == '__main__':
    # WARNING: In production, do NOT use Flask's built-in server.
    # Instead, use a hardened WSGI server (e.g., Gunicorn) run as an unprivileged user:
    #
    #    $ gunicorn --bind 0.0.0.0:443 myapp:app \
    #        --certfile=/etc/ssl/certs/mycert.pem \
    #        --keyfile=/etc/ssl/private/mykey.pem \
    #        --workers=4 \
    #        --user=www-data --group=www-data
    #
    # Ensure the 'images' directory is owned by root (or a different user), permission 0555,
    # and contains only legitimately uploaded, scanned image files.
    #
    # The built-in server here is for development only and will refuse HTTP (Talisman will
    # redirect to HTTPS). Debug mode is off by default.

    app.run(host='0.0.0.0',
            port=443,
            ssl_context=('path/to/fullchain.pem', 'path/to/privkey.pem'),
            debug=False,
            threaded=True)
