from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    filename = request.args.get('file', '')
    if not filename:
        return "No file specified.", 400

    # Prevent directory traversal
    if '/' in filename or '\\' in filename or filename.startswith('.'):
        return "Invalid filename.", 400

    valid_extensions = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    file_ext = filename.rsplit('.', 1)[-1].lower()
    if file_ext not in valid_extensions:
        return "Unsupported file type.", 400

    image_folder = os.path.join(os.path.dirname(__file__), 'images')

    try:
        return app.send_static_file(os.path.join('images', filename))
    except Exception:
        return "File not found.", 404