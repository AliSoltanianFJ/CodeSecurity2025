from flask import Flask, request, redirect
from flask import send_from_directory
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

    # Ensure the filename contains no path separators or parent dir references
    if filename != os.path.basename(filename) or '..' in filename:
        return "Invalid file name", 400

    # Optional: restrict to allowed image file extensions
    ALLOWED_EXTENSIONS = {'.jpg', '.jpeg', '.png', '.gif'}
    ext = os.path.splitext(filename)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        return "Invalid file type", 400

    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, filename)
    if not os.path.isfile(image_path):
        return "File not found", 404

    # Serve the image file securely using send_from_directory
    mimetypes = {
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.png': 'image/png',
        '.gif': 'image/gif'
    }
    return send_from_directory(image_dir, filename, mimetype=mimetypes.get(ext, 'application/octet-stream'))