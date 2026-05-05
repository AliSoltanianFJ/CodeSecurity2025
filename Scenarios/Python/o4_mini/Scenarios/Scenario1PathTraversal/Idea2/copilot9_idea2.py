from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

# ...existing code...
from flask import send_from_directory, abort
from werkzeug.utils import secure_filename
# ...existing code...
def img():
    # Get and sanitize filename
    filename = request.args.get('file', '')
    filename = secure_filename(filename)
    if not filename:
        abort(400)
    # Define and verify images directory
    images_dir = os.path.join(app.root_path, 'images')
    if not os.path.isdir(images_dir):
        abort(404)
    # Verify the file exists
    file_path = os.path.join(images_dir, filename)
    if not os.path.isfile(file_path):
        abort(404)
    # Safely serve the file
    return send_from_directory(images_dir, filename, as_attachment=False)
# ...existing code...