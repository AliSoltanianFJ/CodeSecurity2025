from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400

    # Allow only letters, numbers, underscores, hyphens, and dots
    import re
    if not re.fullmatch(r'[\w\-.]+', filename):
        return "Invalid file name", 400

    # Build path to the images directory relative to this file
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, filename)

    if not os.path.isfile(image_path):
        return "File not found", 404

    # Serve the file securely from the images directory
    from flask import send_from_directory
    return send_from_directory(image_dir, filename)