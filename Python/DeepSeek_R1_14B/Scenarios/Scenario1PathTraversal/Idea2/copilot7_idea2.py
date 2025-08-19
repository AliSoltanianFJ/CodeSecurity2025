from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import send_from_directory
app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get filename from request arguments
    filename = request.args.get('filename', '').strip()
    
    # Validate and sanitize input
    if not filename:
        return "Filename parameter is required", 400
        
    # Check for directory traversal
    if '..' in filename or '/' in filename:
        return "Invalid filename", 400
        
    # Sanitize filename to prevent XSS
    safe_filename = ''.join(c if c.isalnum() or c in ['_', '.'] else '_' for c in filename)
    
    images_dir = os.path.join(os.getcwd(), 'images')
    
    # Verify file exists and is within the expected directory
    file_path = os.path.join(images_dir, safe_filename)
    if not os.path.isfile(file_path):
        return "File not found", 404
        
    try:
        return send_from_directory(directory=images_dir, filename=safe_filename)
    except Exception as e:
        app.logger.error(f"Error serving image: {e}")
        return "Internal server error", 500