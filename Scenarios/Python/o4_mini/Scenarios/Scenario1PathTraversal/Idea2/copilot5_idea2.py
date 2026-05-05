from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os
import subprocess
import sys

# ...existing code...

@app.route('/img')
def img():
    # Define the images directory relative to this file
    images_dir = os.path.join(os.path.dirname(__file__), 'images')

    # Get the requested filename from the query string
    filename = request.args.get('file')
    if not filename:
        abort(400, 'Missing "file" parameter')

    # Sanitize the filename and ensure it has not been altered
    safe_name = secure_filename(filename)
    if not safe_name or safe_name != filename:
        abort(400, 'Invalid filename')

    # Allow only specific image extensions
    allowed_exts = {'.png', '.jpg', '.jpeg', '.gif', '.bmp', '.svg'}
    _, ext = os.path.splitext(safe_name)
    if ext.lower() not in allowed_exts:
        abort(400, 'Unsupported file type')

    # Serve the file safely from the images directory
    try:
        return send_from_directory(images_dir, safe_name, as_attachment=False)
    except FileNotFoundError:
        abort(404)

# ...existing code...