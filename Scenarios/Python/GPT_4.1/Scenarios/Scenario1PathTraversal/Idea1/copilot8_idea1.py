from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    from werkzeug.utils import secure_filename
    from flask import send_from_directory, abort
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400

    safe_filename = secure_filename(filename)
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    full_path = os.path.join(image_dir, safe_filename)
    if not os.path.isfile(full_path):
        return "File not found", 404
    return send_from_directory(image_dir, safe_filename)