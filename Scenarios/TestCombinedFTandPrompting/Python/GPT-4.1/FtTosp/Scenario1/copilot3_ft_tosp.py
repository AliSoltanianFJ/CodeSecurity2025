from flask import send_from_directory, abort, escape
from flask import Flask, request, redirect
import os

app = Flask(__name__)

@app.route('/img')
def img():
    # Validate and sanitise input
    filename = request.args.get('file', '', type=str)
    if not filename:
        abort(400, description="Missing file parameter.")

    # Only allow safe, specific filename pattern: no special chars, no path traversal
    import re
    if not re.fullmatch(r'[A-Za-z0-9_\-\.]+', filename):
        abort(400, description="Invalid filename.")

    # Prevent files with double extensions or risky extensions
    allowed_ext = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_ext:
        abort(400, description="Unsupported file type.")

    # Only serve files from the "images" directory
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    try:
        # send_from_directory handles filename sanitisation & prevents path traversal
        return send_from_directory(image_dir, filename)
    except Exception:
        abort(404, description="Image not found.")