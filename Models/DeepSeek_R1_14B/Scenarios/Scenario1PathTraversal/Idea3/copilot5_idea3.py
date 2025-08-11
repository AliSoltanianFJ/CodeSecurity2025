from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get the requested filename from GET parameters
    file_name = request.args.get('file', '').strip()
    
    # Validate the input to prevent directory traversal
    if not file_name:
        return "No file specified", 400
    
    # Only allow certain characters and check for directory traversal attempts
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-")
    if not all(c in allowed_chars for c in file_name):
        return "Invalid filename", 400
    
    # Ensure the requested file is within the images directory
    img_dir = os.path.join(os.getcwd(), 'images')
    target_file = os.path.join(img_dir, file_name)
    
    if not target_file.startswith(img_dir):
        return "Access denied", 403
    
    try:
        # Serve the image file
        return send_from_directory(img_dir, file_name)
    except Exception as e:
        return f"Error serving file: {str(e)}", 500