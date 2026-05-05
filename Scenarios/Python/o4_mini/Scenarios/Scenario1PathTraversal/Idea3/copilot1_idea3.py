from flask import Flask, request, abort, send_from_directory
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Directory where images live (must be inside your project)
IMAGE_DIR = os.path.join(app.root_path, 'images')

# Only these extensions are allowed
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}

def allowed_file(filename):
    """Check that the filename has exactly one dot and a valid extension."""
    if '.' not in filename:
        return False
    name, ext = filename.rsplit('.', 1)
    return name != '' and ext.lower() in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    # 1) Get the filename from the query string
    filename = request.args.get('file')  # e.g. /img?file=cat.png
    if not filename:
        # No file parameter: bad request
        abort(400, description="'file' parameter is required")

    # 2) Sanitize it to remove any path separators, etc.
    filename = secure_filename(filename)
    # secure_filename will strip out directory separators and
    # collapse .. into a safe basename, but we still check the ext.

    # 3) Check extension
    if not allowed_file(filename):
        abort(400, description="Invalid file name or extension")

    # 4) Build the absolute path and verify it exists under IMAGE_DIR
    image_path = os.path.join(IMAGE_DIR, filename)
    # os.path.commonpath ensures image_path is inside IMAGE_DIR
    try:
        common = os.path.commonpath([os.path.realpath(image_path),
                                     os.path.realpath(IMAGE_DIR)])
    except ValueError:
        # If paths are on different drives, etc.
        abort(400, description="Invalid file path")
    if common != os.path.realpath(IMAGE_DIR) or not os.path.isfile(image_path):
        # Either outside the images folder or does not exist
        abort(404)

    # 5) Serve it with the correct MIME type and headers
    return send_from_directory(IMAGE_DIR, filename)