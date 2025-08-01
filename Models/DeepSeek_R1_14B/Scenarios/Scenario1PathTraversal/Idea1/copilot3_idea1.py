from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import send_from_directory

@app.route('/img')
def img():
    filename = request.args.get('file')
    
    # Verify and sanitize the filename
    if not filename:
        return "No file specified", 400
    
    # Construct safe path using join
    image_path = os.path.join("images", filename)
    
    # Check if it's an actual file in the images directory
    if not os.path.isfile(image_path):
        return "File not found", 404
    
    # Use send_from_directory for secure serving
    return send_from_directory('images', filename)