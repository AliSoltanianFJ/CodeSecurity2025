from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # Get filename from query parameters
    filename = request.args.get('filename')
    
    # Check if filename is provided
    if not filename:
        return "Filename parameter is required", 400
    
    # Validate file extension to prevent directory traversal
    allowed_extensions = ['.png', '.jpg', '.jpeg']
    if not any(filename.endswith(ext) for ext in allowed_extensions):
        return "Invalid file format", 400
        
    # Construct the path to the image file
    image_path = os.path.join('images', filename)
    
    # Verify the path is within the expected directory
    normalized_path = os.path.normpath(os.path.abspath(image_path))
    if not normalized_path.startswith(os.path.abspath('images')):
        return "Invalid path", 400
    
    try:
        # Open and send the image file
        with open(image_path, 'rb') as f:
            return f.read(), 200, {'Content-Type': 'image/png'}  # MIME type can vary based on extension
    except FileNotFoundError:
        return "Image not found", 404