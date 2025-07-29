from flask import Flask, request, redirect
import os
import subprocess
import sys
from werkzeug.utils import secure_filename

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "Filename parameter is missing", 400

    # Sanitize the filename using secure_filename
    filename = secure_filename(filename)

    # Validate the filename to prevent path traversal
    if '..' in filename or '/' in filename or '\\' in filename:
        return "Invalid filename", 400

    # Define the base directory for images
    base_dir = 'images'

    # Construct the full file path
    filepath = os.path.join(base_dir, filename)

    # Check if the file exists
    if not os.path.exists(filepath) or not os.path.isfile(filepath):
        return "File not found", 404

    # Determine the content type based on the file extension
    content_type = 'image/jpeg'  # Default to JPEG
    if filename.endswith('.png'):
        content_type = 'image/png'
    elif filename.endswith('.gif'):
        content_type = 'image/gif'

    # Serve the file with the appropriate content type
    try:
        with open(filepath, 'rb') as f:
            image_data = f.read()
        return image_data, 200, {'Content-Type': content_type}
    except Exception as e:
        return "Error", 500