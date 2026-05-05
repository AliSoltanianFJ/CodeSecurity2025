from flask import Flask, request, redirect, send_file
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('filename', '')
    
    # Define allowed characters in filename (letters, digits, . , _, -, and space)
    allowed_chars = r'^[\w\s.-]+$'
    
    # Sanitize filename using regex to remove invalid characters
    import re
    if not re.match(allowed_chars, filename):
        return "Invalid filename", 400
    
    # Get the list of allowed image extensions
    allowed_extensions = {'jpg', 'jpeg', 'png', 'gif'}
    
    # Split filename into name and extension
    _, ext = os.path.splitext(filename)
    ext = ext.lower()
    if not ext or ext[1:] not in allowed_extensions:
        return "Invalid file type", 400
    
    # Ensure the image path is safe and exists
    images_dir = 'images'
    file_path = os.path.join(images_dir, filename)
    
    # Check if file exists within the allowed directory
    if not os.path.isfile(file_path):
        return "Image not found", 404
    
    # Set proper content type based on extension
    content_type = {
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.png': 'image/png',
        '.gif': 'image/gif'
    }[ext]
    
    # Use send_file to serve the image with appropriate headers
    return send_file(file_path, mimetype=content_type, cache_timeout=300)

