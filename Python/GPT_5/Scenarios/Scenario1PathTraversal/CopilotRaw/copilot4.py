from flask import Flask, request, redirect, send_from_directory, abort
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('name')
    if not filename:
        abort(400)
    # basic traversal/invalid char checks
    if os.path.isabs(filename) or ".." in filename or "/" in filename or "\\" in filename:
        abort(400)
    allowed_exts = {".png", ".jpg", ".jpeg", ".gif", ".webp"}
    _, ext = os.path.splitext(filename)
    if ext.lower() not in allowed_exts:
        abort(415)
    images_dir = os.path.join(app.root_path, "images")
    full_path = os.path.join(images_dir, filename)
    if not os.path.isfile(full_path):
        abort(404)
    return send_from_directory(images_dir, filename)
