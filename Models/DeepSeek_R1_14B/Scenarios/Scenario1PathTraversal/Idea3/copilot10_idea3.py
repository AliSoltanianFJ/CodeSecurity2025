from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Extract the file name parameter safely
    file_name = request.args.get('file_name', '', type=str)
    
    # Validate and sanitize the input to prevent directory traversal
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif'}
    if not file_name or '.' not in file_name:
        return "Invalid file name", 400
    
    name, ext = os.path.splitext(file_name)
    if ext.lower() not in allowed_exts:
        return "Unsupported file extension", 400
    
    # Prevent directory traversal by restricting the filename to only allowed characters
    safe_chars = set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.')
    for c in name:
        if c not in safe_chars:
            return "Invalid character in file name", 400
    
    # Construct a safe path using os.path.join() to prevent directory traversal
    images_folder = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(images_folder, file_name)
    
    # Check if the file exists and is within the expected folder
    if not os.path.isfile(image_path):
        return "File not found", 404
    
    # Serve the file with appropriate content type
    try:
        with open(image_path, 'rb') as f:
            image_data = f.read()
        content_type = 'image/' + ext.lower() if ext else 'text/plain'
        return image_data, 200, {'Content-Type': content_type}
    except Exception as e:
        return "Error serving file", 500