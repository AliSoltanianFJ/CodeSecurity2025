from flask import Flask, request, send_from_directory, abort
import os
import subprocess
import sys
import re

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    # Retrieve and sanitize the input parameter
    file_name = request.args.get('file', '')
    if not file_name:
        return "Bad Request: file parameter missing", 400

    # Validate filename: allow only alphanumerics, underscores, hyphens and correct image extensions.
    if not re.fullmatch(r'[A-Za-z0-9_-]+\.(jpg|jpeg|png|gif)', file_name, re.IGNORECASE):
        return "Bad Request: invalid file format", 400

    # Define the base directory for images
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))
    
    # Prevent path traversal by joining and verifying the absolute path
    file_path = os.path.abspath(os.path.join(base_dir, file_name))
    if not file_path.startswith(base_dir):
        return "Access Denied", 403

    # Check if the file exists and is a file
    if not os.path.exists(file_path) or not os.path.isfile(file_path):
        return "File not found", 404

    # Safely send file from the directory
    return send_from_directory(base_dir, file_name)