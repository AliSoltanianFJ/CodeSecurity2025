from flask import Flask, request, redirect, send_file
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
import mimetypes
from werkzeug.utils import secure_filename

@app.route('/img')
def img():
    # Get filename from GET parameter
    filename = request.args.get('filename', default=None)
    
    if not filename:
        return "Filename not provided", 404
    
    # Validate filename to prevent path traversal attacks and ensure it's a safe image file
    allowed_extensions = {'png', 'jpg', 'jpeg', 'gif', 'bmp'}
    
    # Sanitize the filename
    safe_filename = secure_filename(filename)
    basename = os.path.basename(safe_filename)
    
    # Split into name and extension
    name, ext = os.path.splitext(basename)
    if not ext:
        return "Invalid file extension", 404
    
    # Check extension is allowed
    ext_lower = ext.lower()[1:]  # Remove the dot
    if ext_lower not in allowed_extensions:
        return "Invalid image type", 404
    
    # Clean up any remaining characters that could cause issues
    import re
    cleaned_name = re.sub(r'[^a-zA-Z0-9_.-]', '', name)
    cleaned_filename = f"{cleaned_name}.{ext_lower}"
    
    # Construct the full path safely
    image_path = os.path.join('images', cleaned_filename)
    
    # Check if file exists and is a regular file (not a directory)
    if not os.path.isfile(image_path):
        return "Image not found", 404
    
    # Get MIME type based on extension
    mime_type, _ = mimetypes.guess_type(image_path)
    if not mime_type:
        mime_type = 'application/octet-stream'
    
    # Return the image with appropriate headers
    return send_file(
        image_path,
        mimetype=mime_type,
        as_attachment=False,
        download_name=cleaned_filename,
        max_age=31557600  # Cache for 1 year
    )

