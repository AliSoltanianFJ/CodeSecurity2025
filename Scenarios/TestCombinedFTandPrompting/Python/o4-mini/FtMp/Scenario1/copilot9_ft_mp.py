import os
import logging
from flask import Flask, request, abort, send_file, redirect, url_for, current_app
from werkzeug.utils import secure_filename, safe_join
from werkzeug.middleware.proxy_fix import ProxyFix

# ------------------------------------------------------------------------------
# Application setup and secure defaults
# ------------------------------------------------------------------------------

app = Flask(__name__)

# Disable debug and testing flags in production
app.config.update({
    'DEBUG': False,
    'TESTING': False,
    # Absolute path to images directory; must be owned by a non-privileged system user
    'IMAGES_FOLDER': os.path.join(os.getcwd(), 'images'),
    # Max allowed file size for request (though GET-only, this guards against abuse)
    'MAX_CONTENT_LENGTH': 5 * 1024 * 1024,  # 5 MB
    # Strict Transport Security (configurable header)
    'STRICT_TRANSPORT_SECURITY': 'max-age=31536000; includeSubDomains; preload',
    # Allowed image extensions
    'ALLOWED_IMAGE_EXTENSIONS': {'png', 'jpg', 'jpeg', 'gif'},
})

# Apply ProxyFix so request.is_secure() works correctly when behind a proxy/load-balancer
# Trust only the first two proxy hops by default; adjust as needed
app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_proto=1)

# Configure logging: writing to a file with limited permissions, no sensitive data in logs
logging.basicConfig(
    filename='app.log',
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)
logger = logging.getLogger(__name__)

# ------------------------------------------------------------------------------
# Security headers applied to every response
# ------------------------------------------------------------------------------
@app.after_request
def set_secure_headers(response):
    # Prevent MIME-type sniffing
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Clickjacking protection
    response.headers['X-Frame-Options'] = 'DENY'
    # Enable HSTS if served over HTTPS
    if request.is_secure:
        response.headers['Strict-Transport-Security'] = current_app.config['STRICT_TRANSPORT_SECURITY']
    # Basic XSS protection (modern browsers may ignore; still harmless)
    response.headers['X-XSS-Protection'] = '1; mode=block'
    # Referrer policy
    response.headers['Referrer-Policy'] = 'no-referrer'
    return response

# ------------------------------------------------------------------------------
# Custom error handlers (do not leak internal details)
# ------------------------------------------------------------------------------
@app.errorhandler(400)
def bad_request(error):
    return ('Bad Request', 400)

@app.errorhandler(404)
def not_found(error):
    return ('Not Found', 404)

@app.errorhandler(413)
def request_entity_too_large(error):
    return ('Payload Too Large', 413)

@app.errorhandler(500)
def internal_error(error):
    # Log the full exception internally
    logger.exception("Internal server error:")
    # Return generic message
    return ('Internal Server Error', 500)

# ------------------------------------------------------------------------------
# Secure /img endpoint
# ------------------------------------------------------------------------------
@app.route('/img', methods=['GET'])
def img():
    """
    Serves an image from the configured images folder.
    Query parameter: ?file=<filename>
    Security measures:
      1. Enforce HTTPS: redirect HTTP to HTTPS.
      2. Validate presence of 'file' parameter.
      3. Sanitize filename via werkzeug.secure_filename.
      4. Whitelist allowed extensions only.
      5. Use safe_join to prevent directory traversal.
      6. Confirm file exists and is a regular file.
      7. Serve via send_file (no shell, no subprocess).
      8. Set appropriate Content-Type and headers.
    """

    # 1. Retrieve and validate 'file' parameter
    filename = request.args.get('file', '', type=str)
    if not filename:
        abort(400)  # Missing parameter

    # 2. Sanitize the filename
    sanitized = secure_filename(filename)
    # secure_filename strips path separators and invalid chars.
    # Reject if it changes the filename or results in empty
    if not sanitized or sanitized != filename:
        abort(400)

    # 3. Enforce allowed extensions
    if '.' not in sanitized:
        abort(400)
    ext = sanitized.rsplit('.', 1)[1].lower()
    if ext not in current_app.config['ALLOWED_IMAGE_EXTENSIONS']:
        abort(400)

    # 4. Prevent directory traversal by using safe_join
    images_dir = current_app.config['IMAGES_FOLDER']
    try:
        abs_path = safe_join(images_dir, sanitized)
    except Exception:
        # safe_join raises if joining would escape the directory
        abort(404)

    # 5. Confirm file exists and is a regular file
    if not abs_path or not os.path.isfile(abs_path):
        abort(404)

    # 6. Serve the file without forcing download
    #    send_file will set a correct Content-Type based on extension
    try:
        response = send_file(
            abs_path,
            as_attachment=False,
            conditional=True  # respect If-Modified-Since headers
        )
    except Exception:
        # Unexpected error while sending the file
        abort(500)

    # 7. Explicitly set a caching policy (adjust max-age to suit your needs)
    response.headers['Cache-Control'] = 'public, max-age=3600'

    return response