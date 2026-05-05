from flask import Flask, request, redirect
import os
import subprocess
import sys
import mimetypes
import re

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # Get filename from request arguments
    filename = request.args.get('filename', default=None)
    
    if not filename:
        return "Filename parameter is required", 400

    # Validate the filename to prevent directory traversal attacks
    # Only allow alphanumeric characters, underscores, and dots in the filename
    if not re.match(r'^[\w.-]+$', filename):
        return "Invalid filename", 400

    # Define allowed image file extensions
    allowed_extensions = {'png', 'jpg', 'jpeg', 'gif'}
    
    # Split filename into name and extension
    _, ext = os.path.splitext(filename)
    if ext.lower().lstrip('.') not in allowed_extensions:
        return "Invalid file type", 400

    # Construct the full path to the image file safely
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(images_dir, filename)

    # Check if the constructed path is within the allowed directory
    if not image_path.startswith(images_dir):
        return "Invalid path", 403

    # Verify file exists and is a regular file (not a directory or special file)
    if not os.path.isfile(image_path):
        return "Image not found", 404

    # Check MIME type to confirm it's an image
    try:
        mime_type = mimetypes.guess_type(image_path)[0]
        if not mime_type.startswith('image/'):
            return "Not an image file", 403
    except Exception as e:
        app.logger.error(f"Error checking MIME type: {str(e)}")
        return "Invalid image file", 400

    # Serve the image with proper headers and caching
    try:
        return send_file(
            image_path,
            mimetype=mime_type,
            last_modified=os.path.getmtime(image_path),
            max_age=31536000  # Cache for one year
        )
    except Exception as e:
        app.logger.error(f"Error serving file: {str(e)}")
        return "Internal Server Error", 500

