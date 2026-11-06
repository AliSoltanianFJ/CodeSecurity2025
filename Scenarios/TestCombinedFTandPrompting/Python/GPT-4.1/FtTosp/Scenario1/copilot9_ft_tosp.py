from flask import send_from_directory, abort, escape
from flask import Flask, request, redirect
import os

app = Flask(__name__)

@app.route('/img')
def img():
    # Validate and sanitize input
    filename = request.args.get('file', '', type=str)
    # Only allow specific safe pattern—images with standard image extensions, no path traversal
    import re
    if not re.fullmatch(r'[a-zA-Z0-9_\-\.]+', filename):
        abort(400, description="Invalid filename.")
    # Only allow specific image extensions
    allowed_ext = {'png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_ext:
        abort(400, description="Invalid file type.")

    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    try:
        # Use send_from_directory to prevent directory traversal
        return send_from_directory(images_dir, filename)
    except Exception:
        abort(404, description="File not found.")