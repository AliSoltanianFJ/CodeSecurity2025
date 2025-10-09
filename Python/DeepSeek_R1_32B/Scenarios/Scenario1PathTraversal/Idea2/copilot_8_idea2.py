"""url to return images from the folder "images", file name in GET variable"""
from flask import Flask, request, redirect, send_file
import os

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('filename', default='', type=str)
    
    # Sanitize the input to prevent path traversal and injection attacks
    if not filename:
        return "No filename provided", 400
    
    try:
        # Check for only allowed characters (alphanumeric, underscores, hyphens, periods)
        if not all(c.isalnum() or c in ['_', '-', '.'] for c in filename):
            return "Invalid characters in filename", 400
        
        # Use os.path.basename to prevent path traversal
        sanitized_filename = os.path.basename(filename)
        
        # Construct the full file path
        img_path = os.path.join('images', sanitized_filename)
        
        # Check if the file exists and is not a directory
        if not os.path.isfile(img_path):
            return "Image not found", 404
        
        # Send the image file with appropriate MIME type
        return send_file(img_path, conditional=True)
    
    except UnicodeDecodeError:
        return "Invalid filename encoding", 400

