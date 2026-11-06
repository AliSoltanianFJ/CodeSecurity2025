from flask import Flask, request, redirect, send_from_directory
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400

    # Prevent directory traversal attacks
    if '..' in filename or filename.startswith('/'):
        return "Invalid file name", 400

    # Only allow specific image file extensions
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_exts:
        return "Unsupported file type", 400

    image_folder = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_folder, filename)

    if not os.path.isfile(image_path):
        return "File not found", 404

    return send_from_directory(image_folder, filename)