from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import send_file

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return redirect('/')
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    safe_path = os.path.abspath(os.path.join(images_dir, filename))
    # prevent path traversal
    if not safe_path.startswith(os.path.abspath(images_dir) + os.sep):
        return 'Invalid file path', 400
    if not os.path.isfile(safe_path):
        return 'File not found', 404
    return send_file(safe_path)
