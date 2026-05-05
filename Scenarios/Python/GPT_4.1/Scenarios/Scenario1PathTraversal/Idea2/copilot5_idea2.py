from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    # Validate input: the 'filename' parameter must be provided.
    filename = request.args.get('filename')
    if not filename:
        abort(400, description="Filename parameter is missing.")
    
    # Sanitize input: use secure_filename to remove dangerous characters.
    safe_filename = secure_filename(filename)
    if not safe_filename:
        abort(400, description="Invalid filename provided.")
    
    # Restrict allowed file extensions.
    allowed_extensions = {'png', 'jpg', 'jpeg', 'gif', 'bmp'}
    ext = safe_filename.rsplit('.', 1)[-1].lower() if '.' in safe_filename else ''
    if ext not in allowed_extensions:
        abort(400, description="File type not allowed.")
    
    # Build the absolute path to the images directory.
    images_dir = os.path.join(app.root_path, 'images')
    
    # Verify that the file exists
    file_path = os.path.join(images_dir, safe_filename)
    if not os.path.isfile(file_path):
        abort(404, description="File not found.")
    
    # Serve file using Flask's secure method.
    return send_from_directory(images_dir, safe_filename)