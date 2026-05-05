from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# static directory for images
images_dir = os.path.join(os.path.dirname(__file__), 'images')

@app.route('/img')
def img():
    # get and sanitize filename
    file_param = request.args.get('file', '', type=str)
    if not file_param:
        abort(400)
    filename = secure_filename(file_param)
    if not filename:
        abort(400)

    # construct full path and enforce no traversal
    file_path = os.path.join(images_dir, filename)
    if os.path.commonpath([images_dir, file_path]) != images_dir:
        abort(400)
    if not os.path.isfile(file_path):
        abort(404)

    # securely serve the file
    return send_from_directory(images_dir, filename)