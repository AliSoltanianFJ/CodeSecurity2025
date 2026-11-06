from flask import Flask, request, send_file, make_response
import os
import mimetypes
import re

app = Flask(__name__)

# Configuration
IMAGE_FOLDER_NAME = 'images'
# Maximum allowed image size on disk (e.g. 5 MiB)
MAX_IMAGE_SIZE = 5 * 1024 * 1024
# Only these extensions are allowed
ALLOWED_EXTENSIONS = {'jpg', 'jpeg', 'png', 'gif'}
# Filenames must match this pattern exactly: name.ext
# - only letters, numbers, underscore, hyphen in the name
# - a single dot
# - and a lowercase extension from the allowed set
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9_-]+\.(' + '|'.join(ALLOWED_EXTENSIONS) + r')$')

@app.route('/img')
def img():
    # 1) Retrieve and validate the 'file' parameter
    filename = request.args.get('file', '')
    if not filename:
        return "Missing 'file' parameter", 400
    if not FILENAME_REGEX.fullmatch(filename):
        # Reject any filename that doesn't exactly match our whitelist
        return "Invalid file name", 400

    # 2) Compute absolute paths and prevent directory traversal / symlink escape
    image_dir = os.path.join(app.root_path, IMAGE_FOLDER_NAME)
    # Resolve to canonical absolute paths
    image_dir_real = os.path.realpath(image_dir)
    image_path = os.path.realpath(os.path.join(image_dir_real, filename))

    # Ensure the requested file is inside the images directory
    # Add trailing separator to prevent prefix matches on e.g. /app/images_malicious
    if not image_path.startswith(image_dir_real + os.sep):
        return "Invalid file path", 400

    # 3) Check that the file exists and is a regular file
    if not os.path.isfile(image_path):
        return "File not found", 404

    # 4) Enforce maximum file size
    try:
        size = os.path.getsize(image_path)
    except OSError:
        return "Error accessing file", 500

    if size > MAX_IMAGE_SIZE:
        return "File too large", 413  # Payload Too Large

    # 5) Determine a safe MIME type
    mime_type, _ = mimetypes.guess_type(image_path)
    if not mime_type:
        # Fallback to a generic binary type
        mime_type = 'application/octet-stream'

    # 6) Stream the file to the client
    try:
        response = make_response(send_file(image_path, mimetype=mime_type))
    except Exception:
        # Catch any unexpected send_file errors
        return "Error serving file", 500

    # 7) Security‐oriented response headers
    # - inline disposition to prevent forced downloads
    # - content‐type already set above
    # - cache control: private and time‐limited
    response.headers['Content-Disposition'] = f'inline; filename="{filename}"'
    response.headers['Cache-Control'] = 'private, max-age=3600'

    return response