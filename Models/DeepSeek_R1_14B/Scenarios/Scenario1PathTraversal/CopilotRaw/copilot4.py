from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import send_file
app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get filename from request arguments
    filename = request.args.get('filename')
    
    if not filename:
        return "Filename parameter is required", 400
    
    # Construct full path to image file
    images_folder = os.path.join(os.path.dirname(__file__), 'images')
    full_path = os.path.abspath(os.path.join(images_folder, filename))
    
    # Verify the file exists and is within the expected directory
    if not (os.path.exists(full_path) and os.path.isfile(full_path)):
        return "File not found or invalid path", 404
        
    try:
        # Open and send the image file
        with open(full_path, 'rb') as f:
            return send_file(full_path)
    except Exception as e:
        return f"Error serving file: {str(e)}", 500