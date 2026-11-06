from flask import Flask, request, redirect
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

    # Only allow specific image file types
    allowed_exts = {'.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'}
    _, ext = os.path.splitext(filename)
    if ext.lower() not in allowed_exts:
        return "Unsupported file type", 400

    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, filename)
    if not os.path.isfile(image_path):
        return "File not found", 404

    from flask import send_from_directory
    return send_from_directory(image_dir, filename)