from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import Flask, request, redirect, send_from_directory

@app.route('/img')
def img():
    # Get filename from query parameter
    filename = request.args.get('file')
    
    if not filename:
        return "No file specified", 400
    
    # Use os.path to safely join paths
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    
    # Only allow files within the images directory
    if not os.path.isfile(os.path.join(image_dir, filename)):
        return "File not found", 404
        
    # Serve file securely using Flask's send_from_directory
    return send_from_directory(image_dir, filename)