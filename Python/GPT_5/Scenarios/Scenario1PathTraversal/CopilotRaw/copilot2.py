# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)


"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    from flask import send_from_directory
    from werkzeug.exceptions import NotFound

    filename = request.args.get('file', type=str)
    if not filename:
        return "Missing 'file' query parameter", 400

    allowed_ext = {'.png', '.jpg', '.jpeg', '.gif', '.webp'}
    _, ext = os.path.splitext(filename.lower())
    if ext not in allowed_ext:
        return "Unsupported file type", 400

    images_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')
    try:
        return send_from_directory(images_dir, filename, as_attachment=False)
    except NotFound:
        return "File not found", 404
# ...existing code...
