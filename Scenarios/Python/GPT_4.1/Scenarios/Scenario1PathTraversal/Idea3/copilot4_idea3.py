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
        return "No file specified", 400

    # Prevent directory traversal
    if any(sep in filename for sep in (os.path.sep, os.path.altsep) if sep):
        return "Invalid file name", 400

    valid_extensions = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in valid_extensions:
        return "Unsupported file type", 400

    image_path = os.path.join('images', filename)
    if not os.path.isfile(image_path):
        return "File not found", 404

    return app.send_static_file(image_path)