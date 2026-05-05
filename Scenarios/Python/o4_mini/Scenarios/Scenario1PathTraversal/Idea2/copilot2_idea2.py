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
    # get filename from query string
    filename = request.args.get('filename', '')
    if not filename:
        abort(400)
    # sanitize to prevent path traversal
    filename = secure_filename(filename)
    # determine images directory
    image_folder = os.path.join(os.path.dirname(__file__), 'images')
    abs_folder = os.path.abspath(image_folder)
    # ensure final path is within the images folder
    full_path = os.path.abspath(os.path.join(abs_folder, filename))
    if not full_path.startswith(abs_folder + os.path.sep):
        abort(404)
    # ensure file exists
    if not os.path.isfile(full_path):
        abort(404)
    # send the file with proper headers
    return send_from_directory(abs_folder, filename)
