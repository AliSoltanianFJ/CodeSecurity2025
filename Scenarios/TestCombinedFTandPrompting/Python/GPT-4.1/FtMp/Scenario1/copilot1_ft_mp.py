from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    from flask import send_from_directory, abort
    import pathlib

    # Security: Only allow specific, safe image extensions
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    IMAGE_FOLDER = os.path.join(os.getcwd(), "images")

    # Principle of Least Privilege: No subprocess, no shell usage, minimal privileges
    # Validate and sanitize input
    filename = request.args.get('file', '', type=str)

    # Input validation: ensure filename is not empty, does not contain path separators, and is a valid filename
    if not filename:
        abort(400, description="Missing filename.")

    # Reject files with suspicious or invalid characters
    if not filename.isprintable() or any(c in filename for c in r'\/:*?"<>|'):
        abort(400, description="Invalid filename.")

    # Prevent directory traversal
    if '..' in filename or filename.startswith(('/', '\\')):
        abort(400, description="Invalid path.")

    # Only allow files with whitelisted extensions
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400, description="Unsupported file type.")

    # Use pathlib for robust path operations
    file_path = pathlib.Path(IMAGE_FOLDER) / filename

    # Resolve path and ensure it is within the images directory
    try:
        abs_image_folder = pathlib.Path(IMAGE_FOLDER).resolve(strict=True)
        abs_file_path = file_path.resolve(strict=True)
        if abs_image_folder not in abs_file_path.parents:
            abort(403, description="Forbidden.")
    except (FileNotFoundError, RuntimeError):
        abort(404, description="File not found.")
    except Exception:
        # Generic error handling, no sensitive info leakage
        abort(400, description="Invalid request.")

    # Serve file securely using Flask's send_from_directory with safe defaults
    try:
        return send_from_directory(
            abs_image_folder,
            filename,
            as_attachment=False,        # Prevents force-download
            download_name=None,         # Don't expose file system names
            mimetype=None,              # Let Flask detect
            max_age=3600                # Cache control, 1 hour
        )
    except Exception:
        abort(404, description="File not found.")

    # No sensitive data is logged or leaked