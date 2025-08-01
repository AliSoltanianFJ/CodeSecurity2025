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
    
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, filename)
    
    if not os.path.isfile(image_path):
        return "File not found", 404
        
    # Use Flask's send_from_directory to handle security
    from flask import send_from_directory
    return send_from_directory('images', filename)