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
    - Prevents path traversal (CWE-22)
    - Validates and sanitizes user input
    - Does not leak internal errors
    - Only allows files with safe extensions
    - Principle of least privilege: only reads files, no write/exec
    """
    # Define the allowed directory and extensions
    IMAGES_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "images"))
    ALLOWED_EXTENSIONS = {'.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'}

    # Get the filename from the query parameter
    filename = request.args.get('file', '')

    # Validate filename: must not be empty, must not contain path separators
    if not filename or '/' in filename or '\\' in filename:
        # Do not leak details about validation failure
        return "Invalid file name.", 400

    # Ensure the extension is allowed
    _, ext = os.path.splitext(filename)
    if ext.lower() not in ALLOWED_EXTENSIONS:
        return "File type not allowed.", 400

    # Build the full path and ensure it's within IMAGES_DIR
    requested_path = os.path.abspath(os.path.join(IMAGES_DIR, filename))
    if not requested_path.startswith(IMAGES_DIR + os.sep):
        # Prevent path traversal
        return "Invalid file path.", 400

    # Check if file exists and is a file
    if not os.path.isfile(requested_path):
        return "File not found.", 404

    # Use Flask's send_file for secure file serving
    try:
        from flask import send_file
        # Set as_attachment=False to prevent download prompt
        return send_file(requested_path, as_attachment=False)
    except Exception:
        # Do not leak internal errors
        return "Error serving file.", 500

# ...existing code...