from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    # Get the filename from the query string
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400

    # Sanitize the filename using secure_filename
    from werkzeug.utils import secure_filename
    safe_filename = secure_filename(filename)
    if safe_filename != filename or not safe_filename:
        return "Invalid file name", 400

    # Build the absolute path to the images directory based on the current file's location
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, safe_filename)

    # Verify the file exists in the images directory
    if not os.path.isfile(image_path):
        return "File not found", 404

    # Serve the file safely using send_from_directory
    from flask import send_from_directory
    return send_from_directory(image_dir, safe_filename)