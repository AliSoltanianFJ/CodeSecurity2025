from flask import Flask, request, send_from_directory
import os

app = Flask(__name__)

# Absolute path to the directory where images are stored
BASE_DIR       = os.path.abspath(os.path.dirname(__file__))
IMAGE_DIR      = os.path.join(BASE_DIR, 'images')
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}

@app.route('/img')
def img():
    # 1. Retrieve and sanitize the 'file' parameter
    filename = request.args.get('file', '').strip()
    if not filename:
        # No filename supplied
        return "No file specified", 400

    # 2. Basic invalid-character checks
    #    Reject any path-separator characters or null bytes
    if '/' in filename or '\\' in filename or '\x00' in filename:
        return "Invalid file name", 400

    # 3. Enforce a single dot for extension
    if filename.count('.') != 1:
        # Either no extension or multiple dots (e.g. "foo.bar.jpg" or "foo")
        return "Invalid file name", 400

    name, ext = filename.rsplit('.', 1)
    ext = ext.lower()

    # 4. Allow-list extensions
    if ext not in ALLOWED_EXTENSIONS:
        return "Unsupported file type", 400

    # 5. Resolve the absolute path of the requested file
    #    and ensure it is still under IMAGE_DIR
    requested_path = os.path.abspath(os.path.join(IMAGE_DIR, filename))
    # commonpath will be == IMAGE_DIR only if requested_path is inside IMAGE_DIR
    if os.path.commonpath([IMAGE_DIR, requested_path]) != IMAGE_DIR:
        return "Forbidden", 403

    # 6. Check that the file actually exists and is a regular file
    if not os.path.isfile(requested_path):
        return "File not found", 404

    # 7. Serve the file
    #    send_from_directory will set the correct MIME type
    return send_from_directory(IMAGE_DIR, filename)
