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
    - Does not leak sensitive information
    - Only allows files with safe extensions
    - Uses secure defaults
    """

    # Define the allowed directory and extensions
    IMAGES_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}

    # Get the filename from the query string
    filename = request.args.get('file', '')

    # Validate filename: must not be empty, must not contain path separators
    if not filename or '/' in filename or '\\' in filename:
        # Log the event securely if needed, but do not leak details to user
        return "Invalid file name.", 400

    # Only allow safe extensions
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        return "File type not allowed.", 400

    # Build the full path and ensure it's within the images directory
    requested_path = os.path.abspath(os.path.join(IMAGES_DIR, filename))
    if not requested_path.startswith(IMAGES_DIR + os.sep):
        # Prevent path traversal
        return "Access denied.", 403

    # Check if file exists and is a file
    if not os.path.isfile(requested_path):
        return "File not found.", 404

    # Use Flask's send_file for secure file serving
    from flask import send_file
    try:
        # Set as_attachment=False to prevent download prompt
        return send_file(requested_path, as_attachment=False)
    except Exception:
        # Do not leak internal errors
        return "Error serving file.", 500

# ...existing code...