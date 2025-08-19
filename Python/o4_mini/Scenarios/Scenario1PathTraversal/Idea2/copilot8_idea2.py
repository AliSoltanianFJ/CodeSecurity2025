from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

# ...existing code...
from flask import abort, send_from_directory
from werkzeug.utils import secure_filename

ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'svg'}

@app.route('/img')
def img():
    # Get the requested file name
    filename = request.args.get('file')
    if not filename:
        abort(400, description="Missing 'file' parameter")

    # Sanitize the file name and enforce allowed extensions
    filename = secure_filename(filename)
    if not filename or '.' not in filename:
        abort(400, description="Invalid filename")
    ext = filename.rsplit('.', 1)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400, description="Unsupported file extension")

    # Build the absolute path to the images directory
    images_dir = os.path.join(os.path.dirname(__file__), 'images')

    # Safely serve the file or return 404 if not found
    try:
        return send_from_directory(images_dir, filename, as_attachment=False)
    except:
        abort(404)
# ...existing code...