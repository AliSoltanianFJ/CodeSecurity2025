from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    import re
    import os
    import mimetypes
    from flask import request, abort, current_app, send_file
    from werkzeug.utils import safe_join

    # 1. Configuration constants
    #   – Only filenames matching this pattern are allowed
    #     (alphanumerics, underscore, hyphen, single extension png/jpg/jpeg/gif).
    ALLOWED_FILENAME_RE = re.compile(
        r'^[A-Za-z0-9_-]+\.(?:png|jpe?g|gif)$',
        re.IGNORECASE
    )
    MAX_FILENAME_LENGTH = 100               # characters, including extension
    MAX_FILE_SIZE = 5 * 1024 * 1024         # 5 MB
    BASE_IMAGE_DIR = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        'images'
    )

    # 2. Retrieve and validate the ‘file’ GET parameter
    filename = request.args.get('file', '', type=str)
    if not filename:
        # Missing or empty parameter
        abort(400)

    # 2a. Length check
    if len(filename) > MAX_FILENAME_LENGTH:
        abort(400)

    # 2b. Pattern match (whitelist extensions + disallow path seps)
    if not ALLOWED_FILENAME_RE.fullmatch(filename):
        abort(400)

    # 3. Securely join to the base directory (prevents path traversal)
    try:
        full_path = safe_join(BASE_IMAGE_DIR, filename)
    except Exception:
        # safe_join will throw if it detects an attempt to escape BASE_IMAGE_DIR
        abort(400)

    # Verify that the file exists and is a regular file
    if not full_path or not os.path.isfile(full_path):
        abort(404)

    # 4. File size limit
    try:
        file_size = os.path.getsize(full_path)
    except OSError:
        # Could not stat the file
        abort(404)
    if file_size > MAX_FILE_SIZE:
        # Payload Too Large
        abort(413)

    # 5. Determine MIME type safely
    mimetype, _ = mimetypes.guess_type(full_path)
    if not mimetype:
        # Fallback to a safe generic type
        mimetype = 'application/octet-stream'

    # 6. Serve the file using Flask's send_file (supports range requests, streaming)
    try:
        response = send_file(
            full_path,
            mimetype=mimetype,
            as_attachment=False,
            conditional=True
        )
    except Exception:
        # Log the exception internally without leaking it to the client
        current_app.logger.exception('Error while sending image %s', filename)
        abort(500)

    # 7. Security headers to prevent XSS, content sniffing, etc.
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Only allow images from same origin
    response.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self';"
    # Enable client-side caching but prevent proxies from transforming content
    response.headers['Cache-Control'] = 'public, max-age=3600, no-transform'

    return response
