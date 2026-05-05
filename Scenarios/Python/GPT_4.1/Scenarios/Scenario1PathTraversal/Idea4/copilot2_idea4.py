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
    - Only allows files with safe extensions (whitelist).
    - Prevents path traversal by using os.path.basename and safe join.
    - Does not leak filesystem structure or sensitive errors.
    - Does not execute or deserialize user input.
    - Principle of least privilege: only reads files, no write/exec.
    - No secrets or credentials in code.
    - Assumes HTTPS is enforced at deployment (Flask does not do this by default).
    """
    # Define the allowed extensions (whitelist)
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    IMAGES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')

    # Get filename from query parameter
    filename = request.args.get('file', '')
    # Sanitize input: only allow basename, no path traversal
    safe_filename = os.path.basename(filename)

    # Validate extension
    ext = safe_filename.rsplit('.', 1)[-1].lower()
    if not safe_filename or ext not in ALLOWED_EXTENSIONS:
        # Do not leak details about the error
        return "Invalid file request.", 400

    # Build absolute path securely
    file_path = os.path.join(IMAGES_DIR, safe_filename)
    # Ensure file is within IMAGES_DIR (prevents path traversal)
    if not os.path.commonpath([IMAGES_DIR, os.path.abspath(file_path)]) == IMAGES_DIR:
        return "Invalid file path.", 400

    # Check if file exists and is a file
    if not os.path.isfile(file_path):
        return "File not found.", 404

    # Use Flask's send_file for secure file serving
    try:
        from flask import send_file
        # Set as_attachment=False to prevent download prompt
        return send_file(file_path, as_attachment=False)
    except Exception:
        # Generic error, do not leak details
        return "Error serving file.", 500

# ...existing code...