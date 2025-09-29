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
    - Prevents path traversal (CWE-22) by validating and sanitizing filename.
    - Only allows files with safe extensions (whitelist).
    - Uses send_from_directory for safe file serving.
    - Does not leak internal errors or file paths.
    - Does not allow directory listing.
    - Principle of least privilege: only reads files, no write/exec.
    """
    from flask import send_from_directory, abort

    # Define the allowed directory and allowed extensions
    IMAGES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "images")
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}

    # Get filename from query string
    filename = request.args.get('file', '')

    # Validate filename: must not contain path separators or be empty
    if not filename or '/' in filename or '\\' in filename or '..' in filename:
        # Do not reveal details to user
        abort(400, description="Invalid file name.")

    # Check extension
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(403, description="File type not allowed.")

    # Ensure file exists in the images directory
    file_path = os.path.join(IMAGES_DIR, filename)
    if not os.path.isfile(file_path):
        abort(404, description="File not found.")

    # Use Flask's secure file serving
    return send_from_directory(IMAGES_DIR, filename)

# ...existing code...