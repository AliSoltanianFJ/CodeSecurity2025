from flask import Flask, request, abort, send_file, current_app
from werkzeug.utils import secure_filename
import os
import re
import imghdr

# Configuration (place these at module level or in your app factory)
IMAGE_FOLDER = os.path.join(os.path.abspath(os.path.dirname(__file__)), 'images')
# Maximum image size we will serve (e.g. 5 MiB)
MAX_IMAGE_SIZE = 5 * 1024 * 1024
# Acceptable filename pattern: alphanumerics, dash, underscore, one of the allowed extensions
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9_-]+\.(png|jpe?g|gif)$', re.IGNORECASE)
# Mapping from imghdr result to official mime type
MIME_TYPES = {
    'png': 'image/png',
    'jpeg': 'image/jpeg',
    'gif': 'image/gif',
}

app = Flask(__name__)
app.config['IMAGE_FOLDER'] = IMAGE_FOLDER

# Ensure image directory exists and is not world-writable
if not os.path.isdir(app.config['IMAGE_FOLDER']):
    os.makedirs(app.config['IMAGE_FOLDER'], mode=0o750)
else:
    os.chmod(app.config['IMAGE_FOLDER'], 0o700)

@app.route('/img')
def img():
    """
    Securely serve an image from the images directory.
    GET parameter:
      file: the base filename of an image (no path components)
    """

    # 1) Retrieve user input
    raw_filename = request.args.get('file', '')
    if not raw_filename:
        # Missing parameter
        abort(400)

    # 2) Reject any null bytes or control characters
    if '\x00' in raw_filename or any(ord(c) < 32 for c in raw_filename):
        abort(400)

    # 3) Whitelist the filename via regex
    #    This also enforces that there is exactly one extension which is in our allowed list.
    if not FILENAME_REGEX.fullmatch(raw_filename):
        abort(400)

    # 4) Secure the filename (removes any stray path components)
    filename = secure_filename(raw_filename)
    if not filename:
        abort(400)

    # 5) Construct the absolute path and enforce it is inside our IMAGE_FOLDER
    image_folder = app.config['IMAGE_FOLDER']
    requested_path = os.path.realpath(os.path.join(image_folder, filename))
    # Make sure the resolved path starts with our images directory path + separator
    if not (requested_path.startswith(image_folder + os.sep)):
        abort(400)

    # 6) Verify the file exists and is a regular file
    if not os.path.isfile(requested_path):
        abort(404)

    # 7) Enforce maximum file size
    try:
        file_size = os.path.getsize(requested_path)
    except OSError:
        abort(404)
    if file_size > MAX_IMAGE_SIZE:
        # Payload Too Large
        abort(413)

    # 8) Use imghdr to double-check the actual image type on disk
    detected_type = imghdr.what(requested_path)
    if detected_type not in MIME_TYPES:
        # Not an allowed image type
        abort(404)

    # 9) Serve with explicit MIME type, no attachment, enable conditional GET (ETag, Last-Modified)
    #    This does not expose any filesystem paths in headers or body
    return send_file(
        requested_path,
        mimetype=MIME_TYPES[detected_type],
        as_attachment=False,
        conditional=True,
        cache_timeout=3600  # 1 hour client caching
    )
