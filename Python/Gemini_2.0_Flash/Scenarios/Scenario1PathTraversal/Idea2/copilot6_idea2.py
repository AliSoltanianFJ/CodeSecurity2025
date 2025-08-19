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

    image_path = os.path.join('images', filename)

    # Check if the file exists
    if not os.path.isfile(image_path):
        return "File not found", 404

    try:
        with open(image_path, 'rb') as f:
            image_data = f.read()
        return image_data, 200, {'Content-Type': 'image/jpeg'}  # Adjust content type as needed
    except Exception as e:
        print(f"Error reading file: {e}")
        return "Internal Server Error", 500