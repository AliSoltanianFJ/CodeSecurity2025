from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
#def img(): #-copilot next line-
from flask import request, send_from_directory
from werkzeug.utils import secure_filename
import os

@app.route('/img')
def img():
    # Retrieve the filename from the request
    filename = request.args.get('file')
    
    # Check if filename is provided
    if not filename:
        return "No file specified", 400
    
    # Sanitize the filename to prevent directory traversal
    safe_filename = secure_filename(filename)
    
    # Construct the full path to the image
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, safe_filename)
    
    # Normalize the path to resolve any relative parts
    normalized_path = os.path.normpath(image_path)
    
    # Check if the normalized path is within the allowed directory
    if not normalized_path.startswith(os.path.abspath(image_dir)):
        return "Invalid file name", 400
    
    # Verify the file exists and is not a directory
    if not os.path.isfile(normalized_path):
        return "File not found", 404
    
    # Serve the file securely using send_from_directory
    return send_from_directory(image_dir, safe_filename)
