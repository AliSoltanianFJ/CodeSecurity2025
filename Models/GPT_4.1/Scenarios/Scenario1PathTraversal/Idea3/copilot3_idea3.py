from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    file_name = request.args.get('file')
    if not file_name:
        return "No file specified", 400

    # Prevent directory traversal
    safe_file_name = os.path.basename(file_name)

    image_path = os.path.join('images', safe_file_name)

    if not os.path.exists(image_path):
        return "File not found", 404

    # Only allow certain image extensions
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    _, ext = os.path.splitext(safe_file_name)
    ext = ext.lstrip('.').lower()
    if ext not in allowed_exts:
        return "Unsupported image type", 400

    # Use Flask's send_from_directory to serve the file
    return app.send_from_directory('images', safe_file_name)