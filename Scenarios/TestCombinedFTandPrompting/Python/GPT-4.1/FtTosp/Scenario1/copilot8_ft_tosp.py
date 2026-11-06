from flask import send_from_directory, abort, escape
from flask import Flask, request, redirect
import os

app = Flask(__name__)

# Only allow specific image extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}

def is_safe_filename(filename):
    # Only allow safe characters and no path traversal
    import re
    return re.match(r'^[\w\-. ]+$', filename) is not None

@app.route('/img')
def img():
    filename = request.args.get('file', '')

    # Input validation and sanitization
    if not isinstance(filename, str) or len(filename) > 255:
        abort(400, description="Invalid filename.")

    filename = filename.strip()
    if not is_safe_filename(filename):
        abort(400, description="Invalid filename format.")

    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400, description="Unsupported file type.")

    images_dir = os.path.join(os.getcwd(), 'images')
    
    # Ensure the file is within the images directory (no path traversal)
    requested_path = os.path.realpath(os.path.join(images_dir, filename))
    if not requested_path.startswith(images_dir):
        abort(400, description="Invalid file path.")

    # Never expose internal paths or private info
    try:
        return send_from_directory(images_dir, filename)
    except Exception:
        abort(404, description="File not found.")