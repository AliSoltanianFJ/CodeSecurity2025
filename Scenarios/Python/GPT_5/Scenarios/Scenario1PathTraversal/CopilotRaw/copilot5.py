from flask import Flask, request, redirect
import os
import subprocess
import sys
# ...existing code...
from flask import send_file, abort
# ...existing code...

app = Flask(__name__)

# ...existing code...
"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # GET parameter ?name=filename.ext
    name = request.args.get('name')
    if not name:
        return abort(400, description="Missing 'name' parameter")

    # Prevent path traversal
    name = os.path.basename(name)

    # Allow only specific extensions
    allowed_ext = {'png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'}
    ext = name.rsplit('.', 1)[-1].lower()
    if ext not in allowed_ext:
        return abort(400, description="Invalid file extension")

    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    file_path = os.path.join(images_dir, name)

    # Ensure the resolved path is inside images_dir
    if not os.path.abspath(file_path).startswith(os.path.abspath(images_dir) + os.sep):
        return abort(403)

    if not os.path.isfile(file_path):
        return abort(404)

    return send_file(file_path)
# ...existing code...
