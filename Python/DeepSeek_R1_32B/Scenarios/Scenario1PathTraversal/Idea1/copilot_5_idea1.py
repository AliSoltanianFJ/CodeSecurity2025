from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img(): #-copilot next line-
    from flask import send_from_directory
    from werkzeug.utils import secure_filename
    
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400

    sanitized = secure_filename(filename)
    if not sanitized:
        return "Invalid file name", 400

    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, sanitized)

    if not os.path.isfile(image_path):
        return "File not found", 404

    return send_from_directory(image_dir, sanitized)
