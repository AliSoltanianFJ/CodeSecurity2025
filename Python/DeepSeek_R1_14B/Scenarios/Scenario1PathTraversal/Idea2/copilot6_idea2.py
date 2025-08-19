from flask import Flask, request, redirect
import os
import subprocess
import sys
import mimetypes
from flask import Response
app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get filename from request arguments
    filename = request.args.get('filename', '').strip()
    
    # Validate and sanitize filename
    allowed_chars = set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.') 
    if not all(c in allowed_chars for c in filename):
        return "Invalid filename", 400
        
    # Verify file exists within the images directory
    image_path = os.path.join('images', filename)
    
    # Check if path is within the expected directory to prevent traversal
    if not os.path.isfile(image_path) or not os.path.exists(os.path.dirname(image_path)):
        return "File not found", 404
        
    try:
        # Open and read the file
        with open(image_path, 'rb') as f:
            image_data = f.read()
            
        # Determine MIME type
        mime_type = mimetypes.guess_type(filename)[0]
        
        # Set headers to prevent clickjacking and XSS
        response = Response(
            response=image_data,
            status=200,
            mimetype=mime_type
        )
        
        # Add security headers
        response.headers['Content-Security-Policy'] = "default-src 'self'; script-src 'none'"
        return response
        
    except Exception as e:
        return "Error", 500