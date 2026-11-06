import os
import re
import logging
from flask import Flask, request, send_file, abort
from werkzeug.utils import secure_filename
from flask_talisman import Talisman

# -----------------------------------------------------------------------------
# Application setup
# -----------------------------------------------------------------------------

app = Flask(__name__)

# Enforce secure defaults
# ---------------------------------------------------------------------------
# • Do not expose stack traces or Flask error pages
app.config['DEBUG'] = False
app.config['PROPAGATE_EXCEPTIONS'] = False

# • Limit maximum response size to mitigate resource exhaustion
#   (e.g. a very large image)
app.config['MAX_CONTENT_LENGTH'] = 5 * 1024 * 1024   # 5 MiB

# • Directory for storing images; must be absolute.
#   Configurable via environment; defaults to a non-world-writable path.
IMAGES_DIRECTORY = os.environ.get('IMAGES_DIRECTORY', '/var/www/app/images')
if not os.path.isabs(IMAGES_DIRECTORY):
    raise RuntimeError("IMAGES_DIRECTORY must be an absolute path")
# Ensure the directory exists and is not writable by group/others
if not os.path.isdir(IMAGES_DIRECTORY):
    raise RuntimeError(f"Images directory does not exist: {IMAGES_DIRECTORY}")
if oct(os.stat(IMAGES_DIRECTORY).st_mode & 0o777)[-3:] not in ('700','750','750','755'):
    app.logger.warning("Ensure that IMAGES_DIRECTORY permissions restrict write access")

# -----------------------------------------------------------------------------
# Secure headers and HTTPS enforcement
# -----------------------------------------------------------------------------
# Use Flask-Talisman to add security headers and force HTTPS.
# SEE: https://github.com/google/flask-talisman
csp = {
    'default-src': ["'self'"],
    'img-src':   ["'self'"]
}
Talisman(app,
         content_security_policy=csp,
         force_https=True,
         strict_transport_security=True,
         frame_options='DENY',
         session_cookie_secure=True,
         session_cookie_http_only=True)

# -----------------------------------------------------------------------------
# Logging configuration
# -----------------------------------------------------------------------------
LOG_FILE = os.environ.get('LOG_FILE', '/var/log/app/app.log')
handler = logging.FileHandler(LOG_FILE)
handler.setLevel(logging.INFO)
formatter = logging.Formatter(
    '%(asctime)s %(levelname)s %(remote_addr)s %(message)s',
    datefmt='%Y-%m-%dT%H:%M:%S%z'
)
handler.setFormatter(formatter)
app.logger.addHandler(handler)
app.logger.setLevel(logging.INFO)

# Attach client IP to every log record
@app.before_request
def attach_remote_addr():
    logging.LoggerAdapter(app.logger, {'remote_addr': request.remote_addr})

# -----------------------------------------------------------------------------
# Input validation configuration
# -----------------------------------------------------------------------------
# Only allow filenames matching:
#  • Alphanumeric, underscore, hyphen
#  • One of the approved extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9_-]+\.(?:png|jpg|jpeg|gif)$')

# -----------------------------------------------------------------------------
# Endpoint: /img
# -----------------------------------------------------------------------------
@app.route('/img')
def img():
    """
    Securely serve image files from IMAGES_DIRECTORY.
    Query parameter:
      • file: the name of the image to retrieve

    Security controls applied:
      1. Reject missing or empty parameters.                  (CWE-20)
      2. Validate filename against a strict regex.            (CWE-22)
      3. Sanitize with werkzeug.secure_filename and enforce
         that it did not change the validated input.         (CWE-73)
      4. Resolve realpath and ensure it is within IMAGES_DIRECTORY.
                                                             (CWE-59, CWE-73)
      5. Enforce file existence and regular-file check.      (CWE-73)
      6. Serve via Flask’s send_file to avoid manual IO.     (CWE-73, CWE-434)
      7. Generic error handling via abort and custom handlers
         to avoid information leakage.                       (CWE-209)
      8. Logging of suspicious events without exposing paths
         or internals to clients.                            (CWE-532)
    """
    # 1. Fetch and type-check input
    filename = request.args.get('file', default='', type=str)
    if not filename:
        app.logger.info("Missing 'file' parameter")
        abort(400)

    # 2. Regex validation: ensures only tight whitelist is allowed
    if not FILENAME_REGEX.fullmatch(filename):
        abort(400)

    # 3. Sanitize file name
    safe_name = secure_filename(filename)
    # secure_filename will strip disallowed chars; ensure nothing was stripped
    if safe_name != filename:
        abort(400)

    # 4. Construct and validate absolute path
    #    Prevents path traversal and symlink tricks
    images_root = os.path.realpath(IMAGES_DIRECTORY)
    requested_path = os.path.realpath(os.path.join(images_root, safe_name))
    if not requested_path.startswith(images_root + os.sep):
        abort(403)

    # 5. Confirm file exists and is a regular file
    if not os.path.isfile(requested_path):
        app.logger.info("File not found: %s", safe_name)
        abort(404)

    # 6. Serve file securely
    try:
        # Let Flask set correct content-type; do not attach as download
        return send_file(requested_path, as_attachment=False)
    except Exception:
        # 7. Log full stack trace server-side, but return generic error
        app.logger.exception("Failed to send file: %s", safe_name)
        abort(500)

# -----------------------------------------------------------------------------
# Generic error handlers (no sensitive details returned to client)
# -----------------------------------------------------------------------------
@app.errorhandler(400)
@app.errorhandler(403)
@app.errorhandler(404)
def handle_client_error(error):
    # The status code is honored; no body detail to avoid leaks
    return '', error.code

@app.errorhandler(500)
def handle_server_error(error):
    return '', 500