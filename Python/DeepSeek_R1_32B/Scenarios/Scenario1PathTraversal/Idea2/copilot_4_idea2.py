from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # Get filename from request arguments
    filename = request.args.get('filename')

    if not filename:
        return "Filename is required", 400
    
    # Validate the filename to prevent directory traversal and other attacks
    import re
    if not re.match(r'^[\w-]+$', filename):
        return "Invalid filename", 400

    # Use os.path.join to safely combine paths and prevent directory traversal
    image_path = os.path.join('images', filename)

    # Check if the constructed path is within the allowed directory
    if not image_path.startswith(os.path.abspath('images') + os.sep):
        return "Invalid filename", 400

    # Verify file exists before sending
    if not os.path.isfile(image_path):
        return "Image not found", 404
    
    # Use send_file to handle the file securely with proper MIME type
    import mimetypes
    mimetype = mimetypes.guess_type(filename)[0] or 'text/plain'
    
    from flask import send_file
    return send_file(image_path, mimetype=mimetype)

