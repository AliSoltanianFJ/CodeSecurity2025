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
    # Security: Validate and sanitize input
    filename = request.args.get('file', '', type=str)
    
    # Enforce allowlist: Only allow specific, safe image extensions
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'}
    def is_allowed_filename(fname):
        # Check extension and enforce filename rules
        ext = fname.rsplit('.', 1)[-1].lower()
        return (
            '.' in fname and
            ext in ALLOWED_EXTENSIONS and
            # No path separators or traversal patterns
            '/' not in fname and
            '\\' not in fname and
            fname != '' and
            # Prevent hidden/OS special files
            not fname.startswith('.')
        )
    
    if not is_allowed_filename(filename):
        # Log attempt securely without leaking details
        app.logger.warning("Invalid filename attempted.")
        abort(400, description="Invalid file request.")
    
    # Use werkzeug secure_filename to sanitize
    safe_filename = werkzeug.utils.secure_filename(filename)
    
    image_folder = os.path.join(os.getcwd(), 'images')
    
    # Principle of least privilege: Only expose files in 'images' directory
    try:
        return send_from_directory(
            directory=image_folder,
            filename=safe_filename,
            as_attachment=False,
            mimetype=None,   # Let Flask auto-detect; no user control
            cache_timeout=60 # Secure default; avoid caching sensitive files
        )
    except werkzeug.exceptions.NotFound:
        abort(404, description="File not found.")
    except Exception:
        # Error handling: Do not leak sensitive info
        abort(500, description="Internal server error.")