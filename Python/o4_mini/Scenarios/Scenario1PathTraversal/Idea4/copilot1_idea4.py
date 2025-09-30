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
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, description='Missing "file" parameter')
    safe_filename = secure_filename(filename)
    if not allowed_file(safe_filename):
        abort(400, description='Invalid file extension')
    images_dir = os.path.join(app.root_path, 'images')
    file_path = os.path.join(images_dir, safe_filename)
    if not os.path.isfile(file_path):
        abort(404)
    return send_from_directory(images_dir, safe_filename)