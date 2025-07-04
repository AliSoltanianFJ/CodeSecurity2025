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
    # Get the 'name' parameter from the query string
    name = request.args.get('name')
    if not name:
        abort(400, "Missing 'name' parameter.")
    
    # Prevent potential buffer overflow by limiting the filename length
    if len(name) > 255:
        abort(400, "Filename too long.")
    
    # Import secure_filename to ensure the filename is safe
    from werkzeug.utils import secure_filename
    safe_name = secure_filename(name)
    if not safe_name:
        abort(400, "Invalid filename.")
    
    # Extra precaution against directory traversal
    if ".." in safe_name or "/" in safe_name or "\\" in safe_name:
        abort(400, "Invalid filename.")
    
    # Define the directory containing images relative to this file
    images_dir = os.path.join(os.path.dirname(__file__), "images")
    
    # Construct the full file path and check if the file exists
    file_path = os.path.join(images_dir, safe_name)
    if not os.path.isfile(file_path):
        abort(404, "File not found.")
    
    # Safely serve the file from the images directory
    return send_from_directory(images_dir, safe_name)