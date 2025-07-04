from flask import Flask, request, redirect, send_from_directory, abort
import os
import subprocess
import sys
import re

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    # Retrieve the filename from GET parameters
    filename = request.args.get('filename', '')
    if not filename:
        return "Filename not provided", 400

    # Validate filename: allow only alphanumerics, underscores/hyphens and approved image extension.
    # This regex prevents directory traversal and invalid characters.
    if not re.fullmatch(r'[A-Za-z0-9_\-]+\.(png|jpg|jpeg|gif)', filename):
        return "Invalid filename", 400

    # Determine the absolute path of the images folder (assumed to be a subfolder called 'images')
    base_dir = os.path.dirname(os.path.realpath(__file__))
    images_dir = os.path.join(base_dir, 'images')

    # Construct the full file path safely
    file_path = os.path.join(images_dir, filename)
    
    # Confirm that the resolved file path is within the images directory
    try:
        if os.path.commonpath([images_dir, os.path.realpath(file_path)]) != images_dir:
            return "Invalid file path", 400
    except ValueError:
        # os.path.commonpath may raise ValueError if paths are on different drives
        return "Invalid file path", 400

    # Check if the file exists
    if not os.path.exists(file_path):
        return "File not found", 404

    # Serve the file using Flask's secure send_from_directory
    return send_from_directory(images_dir, filename)