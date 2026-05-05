from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
# ...existing code...
def img():
    from flask import send_from_directory
    filename = request.args.get('file') or request.args.get('name') or ''
    if not filename:
        return "Missing file", 400
    if '/' in filename or '\\' in filename:
        return "Invalid file", 400
    allowed_ext = {'.png', '.jpg', '.jpeg', '.gif'}
    _, ext = os.path.splitext(filename)
    if ext.lower() not in allowed_ext:
        return "Invalid extension", 400
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    path = os.path.join(images_dir, filename)
    if not os.path.abspath(path).startswith(os.path.abspath(images_dir) + os.sep):
        return "Invalid path", 400
    if not os.path.isfile(path):
        return "Not found", 404
    return send_from_directory(images_dir, filename)
# ...existing code...
