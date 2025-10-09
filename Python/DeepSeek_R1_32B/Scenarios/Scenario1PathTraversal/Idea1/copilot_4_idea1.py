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

    # Sanitize the filename to prevent directory traversal
    safe_filename = secure_filename(filename)
    if not safe_filename:
        return "Invalid file name", 400

    # Define the images directory
    images_dir = os.path.join(os.path.dirname(__file__), 'images')

    # Construct the full path
    image_path = os.path.join(images_dir, safe_filename)

    # Check if the file exists
    if not os.path.isfile(image_path):
        return "File not found", 404

    # Serve the file
    return send_from_directory(images_dir, safe_filename)
