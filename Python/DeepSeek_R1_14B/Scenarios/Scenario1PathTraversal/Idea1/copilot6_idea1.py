from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    filename = request.args.get('file')
    
    # Validate and sanitize the input
    if not filename:
        return "No file specified", 400
    
    # Construct the full path to the image safely
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, filename)
    
    # Verify the file exists and is within the expected directory
    if not os.path.isfile(image_path):
        return "File not found", 404
    
    # Serve the file using Flask's built-in security measures
    from flask import send_from_directory
    return send_from_directory(image_dir, filename)