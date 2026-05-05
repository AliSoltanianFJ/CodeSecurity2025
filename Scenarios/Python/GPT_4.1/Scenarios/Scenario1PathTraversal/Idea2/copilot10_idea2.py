from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    # Retrieve and validate the input parameter 'file'
    filename = request.args.get('file')
    if not filename:
        return "Missing 'file' parameter", 400

    # Allow only filenames with safe characters
    import re
    if not re.fullmatch(r'[a-zA-Z0-9_\-\.]+', filename):
        return "Invalid file parameter", 400

    # Allow only expected image file extensions
    allowed_extensions = {'.jpg', '.jpeg', '.png', '.gif'}
    ext = os.path.splitext(filename)[1].lower()
    if ext not in allowed_extensions:
        return "Invalid file extension", 400

    # Define the safe base directory for images
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "images"))
    file_path = os.path.abspath(os.path.join(base_dir, filename))

    # Ensure the file_path is within the images directory to prevent directory traversal
    if not file_path.startswith(base_dir + os.sep):
        return "Access denied", 403

    # Check if the file exists
    if not os.path.exists(file_path):
        return "File not found", 404

    # Use Flask's send_from_directory to serve the file securely
    from flask import send_from_directory
    return send_from_directory(base_dir, filename)