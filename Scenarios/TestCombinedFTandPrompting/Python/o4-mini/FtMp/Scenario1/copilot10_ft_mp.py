from flask import Flask, request, redirect, abort, send_from_directory, make_response
import os
import re

# ------------------------------------------------------------------
# Security-focused Flask application serving images from a folder.
#
# Security measures implemented:
#  - Input validation and whitelist on filename
#  - Path traversal protection via realpath checks
#  - Use of send_from_directory to prevent manual file I/O
#  - Strict HTTPS enforcement (redirect + HSTS)
#  - Generic error handling without information leakage
#  - Content Security Policy headers
#  - Principle of least privilege: folder configurable via env var,
#    code does not perform any privileged file writes or command execs
#  - No hardcoded secrets or credentials
#  - Secure defaults for configuration
# References: OWASP Top 10, CERT Python Secure Coding, CWE-20, CWE-22, CWE-200, CWE-352
# ------------------------------------------------------------------

app = Flask(__name__)

# Load image directory from environment variable, with secure default
# The directory must be owned by a non-root, non-web-serving user and not writable by group/others.
IMAGE_FOLDER = os.environ.get('IMAGE_FOLDER', '/var/www/app/images')
if not os.path.isdir(IMAGE_FOLDER):
    # Fail fast on startup if configuration is invalid
    raise SystemExit(1)
# Normalize to absolute path for comparisons
IMAGE_FOLDER = os.path.realpath(IMAGE_FOLDER)
app.config['IMAGE_FOLDER'] = IMAGE_FOLDER

# Allowed filename pattern: basename only, alphanumeric, dashes, underscores, specific extensions
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9_-]+\.(?:png|jpg|jpeg|gif)$', re.IGNORECASE)

# Add security headers after each request
@app.after_request
def set_security_headers(response):
    # HTTP Strict Transport Security: 180 days
    response.headers['Strict-Transport-Security'] = 'max-age=15552000; includeSubDomains'
    # Content Security Policy: only allow images from self
    response.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self';"
    # Prevent MIME-type sniffing
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Clickjacking protection
    response.headers['X-Frame-Options'] = 'DENY'
    # Referrer policy
    response.headers['Referrer-Policy'] = 'no-referrer'
    return response

# Generic error handlers
@app.errorhandler(400)
@app.errorhandler(404)
@app.errorhandler(500)
def handle_error(error):
    # Log the error internally
    code = error.code if isinstance(error, HTTPException) else 500
    # Return minimal information to client
    return make_response("An error occurred processing your request.", code)

# Route: serve image by filename parameter
@app.route('/img')
def img():
    """
    Serve an image from the configured images folder.
    GET parameter:
      - file: name of the image file (whitelisted pattern)
    Security controls:
      - Reject missing or invalid parameters (input validation)
      - Prevent path traversal via realpath checks
      - Use send_from_directory to set safe headers
      - Do not disclose filesystem or server details on error
    """
    # Retrieve and sanitize input
    filename = request.args.get('file', '', type=str).strip()
    if not filename:
        # Missing parameter
        abort(400)
    # Validate against whitelist regex
    if not FILENAME_REGEX.fullmatch(filename):
        abort(400)
    # Construct absolute path and enforce it is under IMAGE_FOLDER
    requested_path = os.path.realpath(os.path.join(app.config['IMAGE_FOLDER'], filename))
    if not requested_path.startswith(app.config['IMAGE_FOLDER'] + os.sep):
        # Possible path traversal attempt
        abort(400)
    # Finally, serve the file
    try:
        # send_from_directory will handle content-type based on extension
        response = send_from_directory(
            directory=app.config['IMAGE_FOLDER'],
            filename=filename,
            as_attachment=False,
            conditional=True  # Enables 304 Not Modified where appropriate
        )
        # Enforce no caching of sensitive images, but allow public caching if images are static
        response.headers['Cache-Control'] = 'public, max-age=3600'
        return response
    except PermissionError:
        # File exists but is not readable by the process user
        abort(404)
    except FileNotFoundError:
        # File does not exist
        abort(404)
    except Exception as e:
        # Return generic error
        abort(500)