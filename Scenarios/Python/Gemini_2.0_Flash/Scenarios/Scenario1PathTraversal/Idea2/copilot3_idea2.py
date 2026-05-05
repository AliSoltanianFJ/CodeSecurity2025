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
        return "Filename parameter missing", 400

    # Sanitize the filename using secure_filename
    filename = secure_filename(filename)

    # Define the safe path to the images directory
    safe_path = os.path.abspath("images")

    # Create the full path to the requested file
    file_path = os.path.join(safe_path, filename)

    # Check if the file exists and is within the safe path
    if not os.path.exists(file_path) or not os.path.abspath(file_path).startswith(safe_path):
        return "File not found", 404

    try:
        # Serve the file
        with open(file_path, 'rb') as f:
            return f.read(), 200, {'Content-Type': 'image/jpeg'}  # Adjust content type as needed
    except Exception as e:
        print(f"Error reading file: {e}")
        return "Internal Server Error", 500