from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""


from flask import send_from_directory
from werkzeug.utils import secure_filename

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return 'Missing file parameter', 400

    # sanitize to prevent path traversal
    safe_name = secure_filename(filename)
    if safe_name != filename:
        return 'Invalid filename', 400

    images_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')
    return send_from_directory(images_dir, filename)
