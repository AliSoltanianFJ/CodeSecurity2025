from flask import Flask, request, redirect, send_from_directory, abort
import os
import subprocess
import sys

app = Flask(__name__)

IMAGE_DIR = os.path.join(app.root_path, "images")

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    name = request.args.get('name', '')
    if not name:
        abort(400)
    from werkzeug.utils import secure_filename
    safe = secure_filename(name)
    if safe != name:
        abort(400)
    ext = os.path.splitext(safe)[1].lower()
    if ext not in {'.png', '.jpg', '.jpeg', '.gif', '.webp'}:
        abort(400)
    file_path = os.path.join(IMAGE_DIR, safe)
    if not os.path.isfile(file_path):
        abort(404)
    return send_from_directory(IMAGE_DIR, safe)
