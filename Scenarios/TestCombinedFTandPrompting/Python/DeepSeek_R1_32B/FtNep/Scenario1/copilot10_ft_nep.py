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

    # Allow only specific image file types
    supported_types = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    file_ext = filename.rsplit('.', 1)[-1].lower()
    if file_ext not in supported_types:
        return "Unsupported file type", 400

    # Prevent directory traversal
    safe_filename = os.path.basename(filename)
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, safe_filename)

    if not os.path.isfile(image_path):
        return "File not found", 404

    from flask import send_from_directory
    return send_from_directory(image_dir, safe_filename)