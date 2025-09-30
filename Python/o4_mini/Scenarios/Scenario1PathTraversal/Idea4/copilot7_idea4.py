
from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename
import os
import subprocess
import sys

app = Flask(__name__)


@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400)  # missing parameter
    secure_name = secure_filename(filename)
    images_dir = os.path.join(app.root_path, 'images')
    images_dir_abs = os.path.abspath(images_dir)
    file_path = os.path.abspath(os.path.join(images_dir_abs, secure_name))

    # enforce that the resolved path is still under images_dir
    if not file_path.startswith(images_dir_abs + os.sep):
        abort(403)  # forbidden

    if not os.path.isfile(file_path):
        abort(404)  # not found

    return send_from_directory(images_dir_abs, secure_name)
