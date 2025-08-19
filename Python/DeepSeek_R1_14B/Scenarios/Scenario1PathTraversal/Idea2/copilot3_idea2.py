from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    filename = request.args.get('filename', '').strip()
    
    # Validate and sanitize input
    if not filename:
        return "Filename parameter is required", 400
    
    # Check for directory traversal attempts
    if '/' in filename or '\\' in filename:
        return "Invalid filename", 400
        
    # Only allow specific file extensions
    allowed_extensions = {'.png', '.jpg', '.jpeg', '.gif'}
    ext = os.path.splitext(filename)[1]
    if ext not in allowed_extensions:
        return "Unsupported file type", 400
    
    # Sanitize filename
    from flask import utils
    secure_filename = utils.secure_filename(filename)
    
    # Construct path and serve the file
    image_path = os.path.join('images', secure_filename)
    
    if not os.path.exists(image_path) or not os.access(image_path, os.R_OK):
        return "File not found", 404
    from flask import send_file
    return send_file(image_path)