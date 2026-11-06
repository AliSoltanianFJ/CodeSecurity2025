from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    # Security: Only allow authenticated users (if authentication available in the broader app context)
    # Ensure session validation here if applicable
    
    # Validate and sanitize input
    filename = request.args.get('file', default='', type=str)
    
    # Enforce strict whitelisting: only allow specific, safe image extensions
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'}
    # RegEx: allow only alphanumerics, dashes, and underscores in filename; no spaces or special chars
    import re
    if not re.fullmatch(r'[A-Za-z0-9_\-]+', filename):
        # Input failed sanitization
        return "Invalid filename.", 400
    
    # Example: filename = "cat" --> valid; "cat.png" --> invalid (extension not allowed in name)
    # The extension must be appended by the code, not the user
    
    # For demonstration, let's expect an additional 'ext' (extension) parameter
    ext = request.args.get('ext', default='', type=str).lower()
    if ext not in ALLOWED_EXTENSIONS:
        return "Unsupported file type.", 400
    
    # Build safe file name
    safe_filename = f"{filename}.{ext}"
    
    # Prevent path traversal: do not trust any user input for paths
    from werkzeug.utils import secure_filename
    safe_filename = secure_filename(safe_filename)
    
    # Construct absolute, canonical path
    IMAGE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')
    requested_path = os.path.join(IMAGE_DIR, safe_filename)
    canonical_path = os.path.realpath(requested_path)
    
    # Ensure that the file is within the designated images directory
    if not canonical_path.startswith(IMAGE_DIR):
        return "Access denied.", 403
    
    # Principle of least privilege: No write/delete, only read
    # Do not expose directory listing, metadata, or any non-image files
    try:
        from flask import send_file, abort
        
        # Security: Ensure file exists and is a file, not a symlink
        if not os.path.isfile(canonical_path) or os.path.islink(canonical_path):
            return "File not found.", 404
        
        # Never execute or shell out: No use of subprocess, os.system, eval, etc.
        
        # Serve file securely with Flask's send_file
        # Set content headers to prevent XSS, MIME sniffing
        response = send_file(
            canonical_path,
            mimetype=f'image/{ext}',
            as_attachment=False,
            conditional=True
        )
        response.headers['X-Content-Type-Options'] = 'nosniff'
        response.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self';"
        response.headers['X-Frame-Options'] = 'SAMEORIGIN'
        response.headers['Cache-Control'] = 'private, max-age=3600'
        return response
    except Exception:
        # Log the error securely (Never expose internal error details)
        app.logger.warning('Error serving image', exc_info=True)
        abort(500)