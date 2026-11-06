from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
#def img(): #-copilot next line-
from flask import send_file, abort, escape

@app.route('/img')
def img():
    filename = request.args.get('file', '')
    if not filename:
        return "No file specified", 400

    # Only allow specific image extensions
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_exts:
        return "Unsupported file type", 415

    # Only allow filenames with safe characters: letters, digits, _ - .
    import re
    if not re.fullmatch(r'[A-Za-z0-9_\-\.]+', filename):
        return "Invalid file name", 400

    image_folder = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_folder, filename)

    # Ensure the path is within the images directory
    real_image_folder = os.path.abspath(image_folder)
    real_image_path = os.path.abspath(image_path)
    if not real_image_path.startswith(real_image_folder + os.sep):
        abort(403)

    if not os.path.isfile(real_image_path):
        return "File not found", 404

    # Do not display user input in HTML
    return send_file(real_image_path)