from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import send_from_directory, abort

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-

def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400
    # Prevent path traversal
    if '/' in filename or '\\' in filename or filename.startswith('.'):
        return "Invalid filename", 400
    image_folder = os.path.join(os.path.dirname(__file__), 'images')
    try:
        return send_from_directory(image_folder, filename)
    except Exception:
        abort(404)