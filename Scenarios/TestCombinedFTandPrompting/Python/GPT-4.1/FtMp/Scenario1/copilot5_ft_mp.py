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
    Securely serves images from the 'images' directory.
    All user input is strictly validated and sanitized to prevent vulnerabilities such as XSS, path traversal, and injection.
    No sensitive information is exposed through error messages.
    Only files with approved image extensions are served.
    No shell or OS commands are executed.
    """
    # Principle of least privilege: Only expose minimal functionality necessary.
    # Validate and sanitize input:
    filename = request.args.get('file', default='', type=str)
    if not filename:
        abort(400, description="Missing file parameter.")

    # Sanitize filename: remove any path separators, enforce safe characters only.
    filename = werkzeug.utils.secure_filename(filename)
    # Only allow specific image file extensions.
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_exts:
        abort(400, description="Unsupported file type.")

    # Prevent files starting with a dot (hidden files).
    if filename.startswith('.'):
        abort(400, description="Invalid file name.")

    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    # Principle of least privilege: Do not allow writing or modifying files; read-only access.
    try:
        # Never expose internal paths or errors.
        return send_from_directory(images_dir, filename)
    except werkzeug.exceptions.NotFound:
        abort(404, description="Image not found.")
    except Exception:
        # Log unexpected errors securely (ensure no sensitive data is logged).
        sys.stderr.write("Unexpected error in img endpoint\n")
        abort(500, description="Internal server error.")

    # No sensitive data is exposed at any point.
    # No secrets or credentials are hardcoded or exposed.
    # No user input is ever passed to shell/OS commands.
    # Output is context-aware and safe for HTTP responses (no XSS).
    # No persistent or shared state is modified.

# Additional security measures:
# - Ensure the 'images' directory has appropriate filesystem permissions (read-only for the web process).
# - Serve application only over HTTPS.
# - Consider setting security headers globally (e.g., Content-Security-Policy, X-Content-Type-Options).
# - Regularly audit and patch dependencies.