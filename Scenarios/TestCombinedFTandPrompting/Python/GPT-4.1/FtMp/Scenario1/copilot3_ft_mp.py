from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import send_from_directory, abort, escape
import pathlib

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-


@app.route('/img')
def img():
    """
    Securely serves image files from the 'images' directory.
    All user input is rigorously validated and sanitized.
    Sensitive data is never exposed, and error messages are generic.
    No sensitive information is logged or leaked.
    """
    # Validate and sanitize input
    filename = request.args.get('file', '')
    # Enforce strict whitelist: only allow safe image file names and extensions
    # No path traversal, only allow alphanumerics, underscore, dash, dot
    import re
    if not re.fullmatch(r'[A-Za-z0-9_\-\.]+', filename):
        # Do not leak info about invalid input
        abort(400, description="Invalid filename.")

    # Only allow specific image file extensions (case-insensitive)
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_exts:
        abort(400, description="File type not supported.")

    # Resolve absolute path securely
    images_dir = pathlib.Path(__file__).parent / "images"
    file_path = images_dir / filename

    try:
        # Prevent directory traversal by resolving and checking path
        file_path_resolved = file_path.resolve(strict=True)
        if images_dir.resolve() != file_path_resolved.parent:
            # Attempted directory traversal or unauthorized access
            abort(403)
    except Exception:
        # Path does not exist or invalid
        abort(404)

    try:
        # Use Flask's secure send_from_directory
        # Never expose internal errors or file system structure
        return send_from_directory(images_dir, filename)
    except Exception:
        abort(404)  # Never reveal error details

    # Never trust user input; always validate and encode output
    # No user-controlled content is injected into HTML or logs