from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400
    
    # Normalize and validate the filename
    safe_filename = os.path.normpath(filename)
    
    # Ensure the path stays within the images directory
    if not safe_filename.startswith('./'):
        return "Invalid file name", 400

    image_path = os.path.join('images', safe_filename)
    
    # Verify the file exists and is within the expected directory
    if not os.path.isfile(image_path):
        return "File not found", 404
    
    from flask import send_from_directory
    return send_from_directory('images', safe_filename)