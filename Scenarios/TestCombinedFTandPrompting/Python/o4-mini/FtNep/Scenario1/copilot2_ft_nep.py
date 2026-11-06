import os
import re
import mimetypes
from flask import Flask, request, abort, Response
from flask import request, abort, Response

# Compute once, at import time
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGE_DIR = os.path.realpath(os.path.join(BASE_DIR, 'images'))

# Only allow filenames like "foo.png", "bar-123.JPG", etc.
# Disallow path‐separators, spaces, control‐chars, etc.
FILENAME_PATTERN = re.compile(r'^[A-Za-z0-9_-]+\.(?:jpg|jpeg|png|gif)$', re.IGNORECASE)

app = Flask(__name__)

@app.route('/img')
def img():
    # 1) Get and validate the parameter
    filename = request.args.get('file', '')
    if not filename:
        # 400 Bad Request if missing
        abort(400, description='Missing "file" parameter')
    if not FILENAME_PATTERN.match(filename):
        # 400 Bad Request if it doesn’t match the strict whitelist
        abort(400, description='Invalid file name')

    # 2) Resolve the absolute filesystem path
    requested_path = os.path.realpath(os.path.join(IMAGE_DIR, filename))

    # 3) Ensure the path is still inside IMAGE_DIR
    #    (this also defeats symlink→outside attacks)
    if not requested_path.startswith(IMAGE_DIR + os.sep):
        abort(400, description='Invalid file path')

    # 4) Check that it exists and is a regular file
    if not os.path.isfile(requested_path):
        abort(404, description='File not found')

    # 5) Determine a safe MIME type
    mime_type, _ = mimetypes.guess_type(requested_path)
    if not mime_type:
        mime_type = 'application/octet-stream'

    # 6) Stream the file back in 8KB chunks
    def generate():
        with open(requested_path, 'rb') as f:
            chunk = f.read(8192)
            while chunk:
                yield chunk
                chunk = f.read(8192)

    # 7) Optional security headers
    headers = {
        'Cache-Control': 'public, max-age=3600',
        'X-Content-Type-Options': 'nosniff'
    }

    return Response(generate(), mimetype=mime_type, headers=headers)
