from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import send_from_directory, abort
import werkzeug.utils

@app.route('/img')
def img():
    """
    Securely serves image files from the 'images' directory.
    Security Considerations:
    - Validates and sanitizes input to prevent CWE-22 (Path Traversal), CWE-116 (Improper Encoding), and CWE-79 (XSS)
    - Only allows whitelisted image file extensions
    - Does not leak filesystem or server info in errors (CWE-209)
    - Uses Flask's send_from_directory for secure file serving
    - Does not invoke shell or unsafe APIs (CWE-78, CWE-88)
    - No secrets exposed in code/config (CWE-798)
    - Principle of least privilege: No elevated file or process access
    - No user input reflected in output without context-aware encoding
    """
    # Define safe, absolute path to the 'images' directory
    IMAGES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')

    # Define a strict whitelist of allowed image file extensions
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}

    # Retrieve and validate the 'file' query parameter
    file_name = request.args.get('file', '')

    # Input validation: Check length and character set
    if not file_name or len(file_name) > 100:
        # Reject empty or excessively long input
        abort(400, description="Invalid file parameter.")

    # Sanitize: Remove potentially dangerous characters
    file_name = werkzeug.utils.secure_filename(file_name)

    # Validate extension
    file_ext = file_name.rsplit('.', 1)[-1].lower()
    if file_ext not in ALLOWED_EXTENSIONS:
        abort(400, description="Unsupported file type.")

    # Serve the file securely using send_from_directory
    try:
        # Only files within the images folder and with valid names will be served
        return send_from_directory(IMAGES_DIR, file_name)
    except werkzeug.exceptions.NotFound:
        # Do not reveal internal paths or structure
        abort(404, description="File not found.")
    except Exception:
        # Generic error handling, no sensitive info leaked
        abort(500, description="An error occurred.")

# Notes:
# - Ensure the 'images' directory has strict permissions (no public write, least privilege)
# - Only serve static images; never execute or deserialize user-supplied files
# - All files must be validated, and default error responses must not disclose stack traces or configuration info