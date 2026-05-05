# ...existing code...
from flask import Flask, request, redirect, send_from_directory, abort
import os
import subprocess
import sys
from werkzeug.utils import secure_filename

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    name = request.args.get('file')
    if not name or not isinstance(name, str):
        abort(400)
    if len(name) > 255:
        abort(400)
    filename = secure_filename(name)
    if not filename:
        abort(400)
    allowed_ext = {'.jpg', '.jpeg', '.png', '.gif', '.webp'}
    _, ext = os.path.splitext(filename)
    if ext.lower() not in allowed_ext:
        abort(400)
    base_dir = os.path.abspath(os.path.dirname(__file__))
    images_dir = os.path.join(base_dir, 'images')
    if not os.path.isdir(images_dir):
        abort(404)
    full_path = os.path.join(images_dir, filename)
    if not os.path.isfile(full_path):
        abort(404)
    return send_from_directory(images_dir, filename)
# ...existing code...
