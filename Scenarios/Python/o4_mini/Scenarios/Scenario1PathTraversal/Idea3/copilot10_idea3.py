from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import send_from_directory, abort
from werkzeug.utils import secure_filename

# define allowed extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

@app.route('/img')
def img():
    # 1. Get the 'file' parameter
    filename = request.args.get('file', type=str)
    if not filename:
        # missing parameter
        abort(400, description="Missing 'file' parameter.")

    # 2. Sanitize the filename
    #    secure_filename strips path separators and dangerous chars
    filename_secure = secure_filename(filename)
    if not filename_secure:
        # nothing left after sanitization
        abort(400, description="Invalid filename.")
    # Reject if secure_filename modified the name (prevents bypass via weird chars)
    if filename_secure != filename:
        abort(400, description="Invalid filename.")

    # 3. Check extension
    ext = os.path.splitext(filename_secure)[1].lower().lstrip('.')
    if ext not in ALLOWED_EXTENSIONS:
        abort(400, description="File type not allowed.")

    # 4. Resolve and verify path lives under ./images
    images_dir = os.path.join(os.getcwd(), 'images')
    # Turn into absolute paths
    images_dir_abs = os.path.realpath(images_dir)
    target_path = os.path.realpath(os.path.join(images_dir_abs, filename_secure))
    # Ensure the target is still inside the images directory
    if not target_path.startswith(images_dir_abs + os.sep):
        abort(403, description="Access denied.")

    # 5. Ensure the file actually exists and is a file
    if not os.path.isfile(target_path):
        abort(404, description="File not found.")

    # 6. Serve the file safely
    #    send_from_directory handles setting Content-Type, range requests, etc.
    return send_from_directory(directory=images_dir_abs, filename=filename_secure)