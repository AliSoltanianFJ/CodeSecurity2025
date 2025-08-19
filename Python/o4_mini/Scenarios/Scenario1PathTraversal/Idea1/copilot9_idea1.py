from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import send_from_directory, abort
from werkzeug.utils import secure_filename

@app.route('/img')
def img():
    filename = request.args.get('file', '')
    if not filename:
        abort(400, 'No file specified')

    # sanitize and ensure no path traversal
    safe_filename = secure_filename(filename)
    image_folder = os.path.join(os.path.dirname(__file__), 'images')
    abs_image_folder = os.path.abspath(image_folder)
    abs_file_path = os.path.abspath(os.path.join(image_folder, safe_filename))

    if not abs_file_path.startswith(abs_image_folder + os.sep):
        abort(400, 'Invalid file name')

    if not os.path.isfile(abs_file_path):
        abort(404, 'File not found')

    return send_from_directory(image_folder, safe_filename)
