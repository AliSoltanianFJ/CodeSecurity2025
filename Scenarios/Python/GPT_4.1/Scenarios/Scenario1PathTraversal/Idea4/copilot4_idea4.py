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
    - Prevents path traversal (CWE-22) by validating and sanitizing the filename.
    - Only allows files with safe image extensions.
    - Does not leak filesystem structure or sensitive errors.
    - Principle of least privilege: only reads files, no write/exec.
    - No secrets or credentials are exposed.
    - Assumes HTTPS is enforced at deployment (Flask itself does not enforce HTTPS).
    """

    # Define allowed extensions and base directory
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))

    # Get filename from query string
    filename = request.args.get('file', '')

    # Validate filename: must be a basename, no path separators, and safe extension
    if not filename or '/' in filename or '\\' in filename or '..' in filename:
        # Do not reveal details about the error
        return "Invalid file request.", 400

    # Check extension
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        return "File type not allowed.", 400

    # Build absolute path and ensure it's within BASE_DIR
    file_path = os.path.abspath(os.path.join(BASE_DIR, filename))
    if not file_path.startswith(BASE_DIR + os.sep):
        # Prevent directory traversal
        return "Invalid file path.", 400

    # Check if file exists and is a file
    if not os.path.isfile(file_path):
        return "File not found.", 404

    # Serve file securely
    try:
        # Use Flask's send_file for secure file serving
        from flask import send_file
        return send_file(file_path)
    except Exception:
        # Do not leak internal errors
        return "Error serving file.", 500

# ...existing code...