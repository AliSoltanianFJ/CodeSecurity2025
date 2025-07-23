from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

# ...existing code...
from flask import send_from_directory, abort

@app.route('/img')
def img():
    filename = request.args.get('file', '')
    if not filename:
        abort(400, description="Missing 'file' parameter")

    # Disallow any path separators to prevent path traversal
    if os.path.sep in filename or (os.path.altsep and os.path.altsep in filename):
        abort(400, description="Invalid filename")

    # Enforce a reasonable maximum filename length
    if len(filename) > 255:
        abort(400, description="Filename too long")

    # Only allow known safe image extensions
    allowed_ext = {'png', 'jpg', 'jpeg', 'gif'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_ext:
        abort(400, description="Unsupported file extension")

    # Build a normalized absolute path and ensure it's inside the images directory
    images_dir = os.path.normpath(os.path.join(os.path.dirname(__file__), 'images'))
    safe_path = os.path.normpath(os.path.join(images_dir, filename))
    if not safe_path.startswith(images_dir + os.path.sep):
        abort(403, description="Forbidden")

    # Serve the file without exposing any additional info
    return send_from_directory(images_dir, filename, as_attachment=False)
# ...existing code...