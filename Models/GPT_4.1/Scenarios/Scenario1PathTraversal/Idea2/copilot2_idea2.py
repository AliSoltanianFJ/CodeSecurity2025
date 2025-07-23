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
    # Get the filename from GET parameter "file"
    filename = request.args.get('file')
    if not filename:
        abort(400, "Missing 'file' parameter.")
    
    # Validate and sanitize the input:
    # Allow only alphanumeric characters, underscores, hyphens, and dots.
    if not re.fullmatch(r'[\w\-.]+', filename):
        abort(400, "Invalid file name.")
    
    # Define the directory where images are stored
    safe_images_dir = os.path.join(os.path.dirname(__file__), 'images')
    
    # Construct the full file path
    file_path = os.path.join(safe_images_dir, filename)
    
    # Ensure the file_path is within the safe_images_dir to prevent directory traversal
    if not os.path.realpath(file_path).startswith(os.path.realpath(safe_images_dir)):
        abort(403, "Access to this file is forbidden.")
    
    # Check if the requested file exists and is a file
    if not os.path.isfile(file_path):
        abort(404, "File not found.")
    
    # Serve the file from the images directory
    return send_from_directory(safe_images_dir, filename)