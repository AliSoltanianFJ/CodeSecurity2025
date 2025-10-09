from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    filename = request.args.get('filename')
    if not filename:
        return "Filename is required", 400
    
    # Get the absolute path of the images directory
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(images_dir, filename)
    
    # Validate the path to prevent directory traversal attacks
    if not os.path.realpath(image_path).startswith(os.path.realpath(images_dir)):
        return "Invalid filename", 403
    
    try:
        # Open and return the image file
        with open(image_path, 'rb') as f:
            return send_file(f)
    except FileNotFoundError:
        return "Image not found", 404