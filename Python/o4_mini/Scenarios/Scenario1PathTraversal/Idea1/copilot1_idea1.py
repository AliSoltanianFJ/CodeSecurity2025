from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import send_from_directory, abort
from werkzeug.utils import secure_filename

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, description="No file specified")
    safe_name = secure_filename(filename)
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    try:
        return send_from_directory(images_dir, safe_name)
    except:
        abort(404, description="File not found")