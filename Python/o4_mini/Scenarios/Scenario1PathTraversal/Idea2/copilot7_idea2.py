from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import send_from_directory, abort
from werkzeug.utils import secure_filename
# ...existing code...
@app.route('/img')
def img():
    filename = request.args.get('file', '', type=str)
    if not filename:
        abort(400)
    filename = secure_filename(filename)
    allowed_ext = {'png', 'jpg', 'jpeg', 'gif'}
    if '.' not in filename or filename.rsplit('.', 1)[1].lower() not in allowed_ext:
        abort(400)
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    try:
        return send_from_directory(images_dir, filename)
    except FileNotFoundError:
        abort(404)
# ...existing code...