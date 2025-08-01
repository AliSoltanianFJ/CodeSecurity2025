from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import Flask, request, redirect, send_from_directory

def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400
    
    # Sanitize the filename
    safe_filename = os.path.basename(filename)
    safe_filename = os.path.normpath(safe_filename)  # Remove any path separators or dot-dots
    
    image_folder = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_folder, safe_filename)
    
    if not os.path.isfile(image_path):
        return "File not found", 404
        
    return send_from_directory(image_folder, safe_filename)