from flask import Flask, request, redirect
import os
import subprocess
import sys
import bleach
from flask import send_from_directory
app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get the requested file name from query parameters
    file_name = request.args.get('file', '')
    
    # Validate and sanitize input
    if not file_name:
        return redirect('/error/invalid-image-request')
    
    # Sanitize filename to prevent path traversal attacks
    safe_file_name = bleach.clean(file_name, tags=[], attributes=[], strip=True)
    
    # Define the base directory for images
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    
    # Ensure file stays within the designated images directory
    full_path = os.path.join(image_dir, safe_file_name)
    if not os.path.isfile(full_path):
        return redirect('/error/invalid-image-request')
    
    try:
        # Use send_file to safely serve static files
        return send_from_directory(image_dir, safe_file_name)
    except Exception as e:
        print("Error serving image")
        return redirect('/error/image-serving-failed')