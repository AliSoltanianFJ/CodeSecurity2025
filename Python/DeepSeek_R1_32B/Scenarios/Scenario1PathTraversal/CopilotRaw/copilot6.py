from flask import Flask, request, redirect, send_file
import os
import subprocess
import sys

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('filename')
    if not filename:
        return "Filename not provided", 400
    
    # Validate the filename to prevent directory traversal
    if '../' in filename or os.path.isabs(filename):
        return "Invalid filename", 400
    
    # Construct the full path to the image
    images_folder = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(images_folder, filename)
    
    # Check if the file exists
    if not os.path.isfile(image_path):
        return "Image not found", 404
    
    try:
        return send_file(image_path)
    except Exception as e:
        return f"Error serving image: {str(e)}", 500
