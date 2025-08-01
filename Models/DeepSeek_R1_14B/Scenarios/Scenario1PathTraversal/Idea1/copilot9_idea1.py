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
    
    # Validate filename to prevent directory traversal
    if '..' in os.path.normpath(filename) or '/' in filename:
        return "Invalid file name", 400
    
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    
    try:
        return send_from_directory(image_dir, filename)
    except FileNotFoundError:
        return "File not found", 404