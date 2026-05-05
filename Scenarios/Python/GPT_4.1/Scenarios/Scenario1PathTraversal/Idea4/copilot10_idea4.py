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
    - Only allows files with safe image extensions.
    - Uses Flask's send_from_directory for safe file serving.
    - Does not leak filesystem structure or sensitive errors.
    - Principle of least privilege: only serves files from 'images' directory.
    - No secrets or credentials hardcoded.
    """
    from flask import send_from_directory, abort

    # Define allowed extensions (whitelist)
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    IMAGES_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))

    # Get filename from query string
    filename = request.args.get('file', '')

    # Validate filename: must not be empty, must not contain path separators
    if not filename or '/' in filename or '\\' in filename or filename.startswith('.'):
        # Do not leak details about validation failure
        abort(400)

    # Only allow safe extensions
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(403)

    # Use Flask's secure file serving
    try:
        return send_from_directory(IMAGES_DIR, filename, as_attachment=False)
    except FileNotFoundError:
        abort(404)
    except Exception:
        # Generic error, do not leak details
        abort(500)

# ...existing code...