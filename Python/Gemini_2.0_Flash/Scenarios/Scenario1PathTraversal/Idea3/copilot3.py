from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    from flask import send_from_directory, abort

    filename = request.args.get('file')
    if not filename:
        return "Missing file parameter.", 400

    from werkzeug.utils import secure_filename

    filename = secure_filename(filename)
    if filename == "":
        return "Invalid file name.", 400

    images_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')

    try:
        return send_from_directory(images_dir, filename)
    except Exception:
        abort(404, description="File not found.")