from flask import Flask, request, abort, send_from_directory, make_response, safe_join
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# Disable debug to prevent information leakage
app.config['DEBUG'] = False
app.config['TESTING'] = False

# Directory where images are stored (must exist and be non-writable by the web process)
IMAGE_DIR = os.path.join(app.root_path, 'images')

# Allowed image extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'webp'}

# Maximum file size (in bytes) to serve (5 MiB)
MAX_IMAGE_SIZE = 5 * 1024 * 1024

# Apply global security headers
@app.after_request
def set_security_headers(response):
    # Remove server version header
    response.headers.pop('Server', None)
    # Prevent MIME-sniffing
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Restrict content sources (only allow our own images)
    response.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self';"
    # Prevent caching of potentially sensitive images
    response.headers['Cache-Control'] = 'private, max-age=0, no-store'
    return response

# Error handlers that avoid exposing internal details
@app.errorhandler(400)
def handle_400(error):
    return make_plain_text_response('Bad Request', 400)

@app.errorhandler(404)
def handle_404(error):
    return make_plain_text_response('Not Found', 404)

@app.errorhandler(413)
def handle_413(error):
    return make_plain_text_response('File Too Large', 413)

def make_plain_text_response(message, status_code):
    """Helper to return a plain-text error without revealing internals."""
    response = make_response(message, status_code)
    response.headers['Content-Type'] = 'text/plain; charset=utf-8'
    return response

@app.route('/img', methods=['GET'])
def img():
    # 1. Extract and sanitize the user-supplied filename parameter
    filename = request.args.get('file', '').strip()
    if not filename:
        abort(400)

    # 2. Reject any path separators to prevent directory traversal
    if '/' in filename or '\\' in filename or '\x00' in filename:
        abort(400)

    # 3. Use secure_filename to remove unsafe characters
    safe_name = secure_filename(filename)
    if not safe_name or safe_name != filename:
        abort(400)

    # 4. Enforce allowlist of extensions
    parts = filename.rsplit('.', 1)
    if len(parts) != 2:
        abort(400)
    ext = parts[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400)

    # 5. Safely join the IMAGE_DIR and verify the path
    try:
        # safe_join returns None or raises if invalid
        full_path = safe_join(IMAGE_DIR, filename)
    except Exception:
        abort(400)
    if full_path is None or not os.path.isfile(full_path):
        abort(404)

    # 6. Enforce maximum file size
    try:
        size = os.path.getsize(full_path)
    except OSError:
        abort(404)
    if size > MAX_IMAGE_SIZE:
        abort(413)

    # 7. Serve the file with correct MIME type
    #    send_from_directory prevents header injection and sets headers securely
    return send_from_directory(
        IMAGE_DIR,
        filename,
        as_attachment=False,
        conditional=True  # enable 304 Not Modified when appropriate
    )
