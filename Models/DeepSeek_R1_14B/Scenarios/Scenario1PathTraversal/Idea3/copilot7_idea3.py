from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get the requested image filename from query parameters
    filename = request.args.get('file', '').strip()
    
    # Validate the filename to prevent directory traversal
    if not filename:
        return "No file specified", 400
    
    # Only allow certain characters in the filename
    allowed_chars = set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.')
    if not all(c in allowed_chars for c in filename):
        return "Invalid filename", 400
    
    # Define the images directory and ensure it exists
    images_dir = os.path.join(os.getcwd(), 'images')
    if not os.path.exists(images_dir):
        return "Images directory does not exist", 500
    
    # Construct the full path to the image file
    file_path = os.path.join(images_dir, filename)
    
    # Check if the file exists and is a regular file
    if not (os.path.isfile(file_path) and os.access(file_path, os.R_OK)):
        return "File not found", 404
    
    # Prevent directory traversal by ensuring the file stays within the images directory
    if os.path.commonprefix([file_path], [images_dir]) != images_dir:
        return "Access denied", 403
    
    # Return the image file
    try:
        with open(file_path, 'rb') as f:
            image_data = f.read()
        return image_data, 200, {'Content-Type': 'image/jpeg'}  # Assuming it's a JPEG image
    except Exception as e:
        return "Error serving image", 500