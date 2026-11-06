from flask import Flask, request, abort, send_file
from werkzeug.utils import secure_filename
import os
import mimetypes

app = Flask(__name__)

# Disable debug mode in production
app.config['DEBUG'] = False

# Configuration
IMAGES_DIR = os.path.join(app.root_path, 'images')
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}
MAX_FILE_SIZE = 5 * 1024 * 1024  # 5 MiB

# Precompute real path of images directory and ensure it exists
IMAGES_DIR_REALPATH = os.path.realpath(IMAGES_DIR)
if not os.path.isdir(IMAGES_DIR_REALPATH):
    # In production you might want to log this error rather than raising
    raise RuntimeError("Images directory does not exist or is not a directory: %s" % IMAGES_DIR_REALPATH)

def is_allowed_filename(filename: str) -> bool:
    """
    Check that the filename:
    - Is not empty
    - Contains only a secure basename (no path components)
    - Has an allowed extension
    """
    if not filename:
        return False

    # Use Werkzeug to strip any unsupported characters,
    # and ensure the sanitized name matches exactly the original:
    filename_san = secure_filename(filename)
    if not filename_san or filename_san != filename:
        return False

    # Ensure only one dot, and that the extension is in our whitelist
    if '.' not in filename:
        return False
    name_part, ext = filename.rsplit('.', 1)
    if not name_part:
        return False
    if ext.lower() not in ALLOWED_EXTENSIONS:
        return False

    return True

@app.errorhandler(400)
@app.errorhandler(403)
@app.errorhandler(404)
@app.errorhandler(413)
def handle_error(error):
    """
    Return a minimal response for errors, without exposing internal details.
    """
    # You could return JSON here if your API is JSON-based.
    return ('', error.code)

@app.after_request
def remove_server_header(response):
    """
    Remove Server and other headers that may reveal internal implementation.
    """
    response.headers.pop('Server', None)
    return response

@app.route('/img')
def img():
    # 1) Extract and validate the 'file' parameter
    filename = request.args.get('file', '', type=str)
    if not is_allowed_filename(filename):
        # 400: Bad Request for any malformed or disallowed filename
        abort(400)

    # 2) Construct the absolute, canonical path
    requested_path = os.path.join(IMAGES_DIR_REALPATH, filename)
    requested_realpath = os.path.realpath(requested_path)

    from werkzeug.utils import secure_filename
    # Sanitize the filename to prevent directory traversal and other attacks
    requested_realpath = secure_filename(requested_realpath)

    # 3) Ensure the real path is under our images directory
    if not requested_realpath.startswith(IMAGES_DIR_REALPATH + os.sep):
        # 403: Forbidden for any attempt at directory traversal
        abort(403)

    # 4) Ensure the file exists and is a regular file
    if not os.path.isfile(requested_realpath):
        abort(404)

    # 5) Enforce a maximum file size
    file_size = os.path.getsize(requested_realpath)
    if file_size > MAX_FILE_SIZE:
        # 413: Payload Too Large
        abort(413)

    # 6) Determine the correct MIME type
    mime_type, _ = mimetypes.guess_type(requested_realpath)
    if not mime_type:
        # Fallback to a generic binary
        mime_type = 'application/octet-stream'

    # 7) Serve the file safely
    #    Flask’s send_file handles streaming in chunks to prevent memory bloat.
    return send_file(
        requested_realpath,
        mimetype=mime_type,
        as_attachment=False,
        conditional=True  # Enables HTTP 304 Not Modified when appropriate
    )
