import os
import re
from flask import Flask, request, redirect, url_for, abort, send_from_directory, make_response
from werkzeug.utils import safe_join
from werkzeug.exceptions import HTTPException

# -----------------------
# Configuration & Setup
# -----------------------

app = Flask(__name__)

# Security-sensitive configuration via environment variables or sane defaults
app.config['IMAGE_DIR'] = os.environ.get('IMAGE_DIR', '/var/www/app/images')  # must be absolute path
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024   # limit responses to 10 MiB
app.config['PREFERRED_URL_SCHEME'] = 'https'          # for URL generation
app.config['DEBUG'] = False                           # ensure debug is off in production

# Compile a strict regex for filenames: allow only alphanumeric, dash, underscore; one dot before a known extension
ALLOWED_FILENAME = re.compile(r'^[A-Za-z0-9_-]+\.(?:jpg|jpeg|png|gif|webp)$', re.IGNORECASE)

# -----------------------
# Global Security Headers
# -----------------------

@app.after_request
def set_security_headers(response):
    # Prevent MIME-type sniffing
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Prevent clickjacking
    response.headers['X-Frame-Options'] = 'DENY'
    # Basic Content Security Policy
    response.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self';"
    # Referrer policy to avoid leaking URLs
    response.headers['Referrer-Policy'] = 'no-referrer'
    return response

# -----------------------
# Error Handlers
# -----------------------

@app.errorhandler(400)
@app.errorhandler(404)
@app.errorhandler(413)  # request entity too large
def handle_client_errors(error):
    # Do not reveal internal details; provide generic message
    return ('Bad request or resource not found.', error.code)

@app.errorhandler(500)
def handle_server_error(error):
    return ('Internal server error.', 500)

# -----------------------
# Helper Functions
# -----------------------

def enforce_https():
    """Redirect to HTTPS if the incoming request is not secure."""
    if not request.is_secure:
        # Build HTTPS URL preserving path and query
        url = url_for(request.endpoint,
                      **request.view_args,
                      _scheme='https',
                      _external=True)
        return redirect(url, code=301)
    return None

# -----------------------
# img() Endpoint
# -----------------------

@app.route('/img', methods=['GET'])
def img():
    """
    Serve an image file from a controlled directory.
    Query parameter:
      - name: the image filename to retrieve (e.g. ?name=sample.png)
    Security measures:
      - Enforce HTTPS
      - Validate filename against a strict allowlist regex
      - Prevent directory traversal with safe_join
      - Check that the final path is inside IMAGE_DIR
      - Use Flask's send_from_directory (with conditional GET support)
      - Do not reveal filesystem paths in error responses
    """
    # 1. Enforce HTTPS transport
    https_redirect = enforce_https()
    if https_redirect:
        return https_redirect

    # 2. Retrieve and validate input
    filename = request.args.get('name', type=str)
    if not filename:
        abort(400)

    # 3. Strict filename validation against a whitelist regex
    if not ALLOWED_FILENAME.fullmatch(filename):
        abort(400)

    # 4. Resolve to absolute safe path; prevents directory traversal
    image_dir = app.config['IMAGE_DIR']
    try:
        # safe_join returns None if the join would escape the directory
        safe_path = safe_join(image_dir, filename)
    except Exception:
        abort(400)

    if not safe_path:
        abort(400)

    # 5. Ensure the file exists and is a regular file
    if not os.path.isfile(safe_path):
        abort(404)

    # 6. Serve the file using send_from_directory (adds conditional GET support)
    #    We disable caching at the edge if desired by setting appropriate headers
    try:
        response = make_response(
            send_from_directory(
                directory=image_dir,
                filename=filename,
                as_attachment=False  # inline display
            )
        )
        # Optionally add cache control headers (tunable per deployment policy)
        response.headers['Cache-Control'] = 'public, max-age=3600, no-transform'
        return response
    except HTTPException as e:
        # send_from_directory already aborts with 404/413; propagate
        raise
    except Exception:
        # Unexpected errors should be treated as server errors
        abort(500)
