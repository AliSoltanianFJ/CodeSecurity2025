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
    
    # Validate file extension to prevent directory traversal
    allowed_extensions = ['.png', '.jpg', '.jpeg']
    if not any(filename.endswith(ext) for ext in allowed_extensions):
        return "Invalid file format", 400
        
    # Construct the full path to the image
    images_folder = os.path.join(os.path.dirname(__file__), 'images')
    file_path = os.path.abspath(os.path.join(images_folder, filename))
    
    # Verify the file exists and is within the expected directory
    if not (os.path.exists(file_path) and os.path.isfile(file_path)):
        return "File not found", 404
        
    try:
        with open(file_path, 'rb') as f:
            image_data = f.read()
            
        # Determine MIME type based on file extension
        mime_type = 'image/png' if filename.endswith('.png') else \
                    'image/jpeg' if filename.endswith(('.jpg', '.jpeg')) else None
                    
        return send_file(file_path, mimetype=mime_type)
        
    except Exception as e:
        return f"Error serving image: {str(e)}", 500