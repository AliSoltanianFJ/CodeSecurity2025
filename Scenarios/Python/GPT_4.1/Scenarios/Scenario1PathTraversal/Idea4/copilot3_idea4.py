from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

# ...existing code...

@app.route('/img')
def img():
    """
    Securely serves images from the 'images' directory.
    Security decisions:
    - Prevents path traversal by validating and sanitizing the filename.
    - Only allows files with safe image extensions.
    - Uses send_from_directory for safe file serving.
    - Does not leak internal errors or file system structure.
    - Does not allow directory listing or access outside 'images'.
    """

    from flask import send_from_directory, abort

    # Define allowed extensions (whitelist)
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    IMAGES_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))

    # Get filename from query string
    filename = request.args.get('file', '')

    # Validate filename: must not be empty, must not contain path separators, must have allowed extension
    if not filename:
        abort(400, description="Missing file parameter.")

    # Prevent path traversal and suspicious filenames
    if '/' in filename or '\\' in filename or filename.startswith('.'):
        abort(400, description="Invalid filename.")

    # Check extension
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(403, description="File type not allowed.")

    # Use send_from_directory to serve file securely
    try:
        return send_from_directory(IMAGES_DIR, filename, as_attachment=False)
    except FileNotFoundError:
        abort(404, description="File not found.")
    except Exception:
        # Log the error internally if needed, but do not leak details to user
        abort(500, description="Internal server error.")

# ...existing code...