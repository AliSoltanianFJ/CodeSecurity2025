from flask import send_from_directory, abort, escape
import werkzeug.utils
from flask import Flask, request, redirect
import os

app = Flask(__name__)

def img():
    # Validate: Only allow specific, safe filename patterns (e.g., alphanumeric, underscores, hyphens, dot).
    filename = request.args.get('file', '', type=str)
    if not filename:
        abort(400, description="Missing filename.")
    
    # Clean: Remove any potentially dangerous characters.
    filename = filename.strip()
    # Only allow safe characters (no directory traversal, no special chars)
    import re
    if not re.match(r'^[\w\-\.]+$', filename):
        abort(400, description="Invalid filename.")
    
    # Prevent buffer overflow: Limit filename length.
    MAX_FILENAME_LENGTH = 255
    if len(filename) > MAX_FILENAME_LENGTH:
        abort(400, description="Filename too long.")

    # Prevent XSS: Do not reflect input; error messages are generic.
    # Prevent insecure deserialization: Never deserialize user input.

    # Only serve files with safe, known extensions
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400, description="File type not allowed.")

    # Use Werkzeug's secure_filename to sanitize filename
    filename = werkzeug.utils.secure_filename(filename)

    images_dir = os.path.join(os.path.dirname(__file__), "images")

    # Never reveal private info; never list directory contents.

    try:
        # Use send_from_directory to safely serve files
        return send_from_directory(images_dir, filename)
    except Exception:
        abort(404, description="File not found.")