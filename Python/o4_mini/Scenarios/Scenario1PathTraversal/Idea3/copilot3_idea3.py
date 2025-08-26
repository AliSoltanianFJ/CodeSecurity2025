import os
from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename

# 1. Configuration
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGES_DIR = os.path.join(BASE_DIR, 'images')

# Only these extensions are served
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'webp'}

app = Flask(__name__)

def is_allowed_filename(filename: str) -> bool:
    """
    Return True if filename has one extension from ALLOWED_EXTENSIONS
    """
    # secure_filename strips path separators and unsafe chars
    clean = secure_filename(filename)
    if not clean:
        return False
    # Must have exactly one dot and a valid extension
    parts = clean.rsplit('.', 1)
    return len(parts) == 2 and parts[1].lower() in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    # 2. Fetch & validate input
    raw = request.args.get('file', '')
    if not raw:
        abort(400, description="'file' parameter is required")
    if not is_allowed_filename(raw):
        abort(400, description="Invalid filename or extension")

    filename = secure_filename(raw)  # e.g. "cat.png"

    # 3. Construct absolute path and ensure it's under IMAGES_DIR
    requested_path = os.path.abspath(os.path.join(IMAGES_DIR, filename))
    images_dir_abs = os.path.abspath(IMAGES_DIR)

    # Prevent directory traversal or symlink escape
    if not requested_path.startswith(images_dir_abs + os.sep):
        abort(403, description="Access denied")

    # 4. Ensure file exists and is a regular file
    if not os.path.exists(requested_path) or not os.path.isfile(requested_path):
        abort(404, description="Image not found")

    # 5. Serve the file directly from the images folder
    #    Flask’s send_from_directory also does a safe join internally,
    #    but we’ve already double-checked the path above.
    return send_from_directory(
        directory=images_dir_abs,
        filename=filename,
        as_attachment=False,
        mimetype=None  # let Flask guess based on extension
    )