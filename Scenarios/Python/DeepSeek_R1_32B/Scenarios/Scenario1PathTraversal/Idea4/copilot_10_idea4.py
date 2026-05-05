from flask import Flask, request, redirect, send_from_directory
import os

app = Flask(__name__)

IMAGES_DIR = "images"

@app.route('/img')
def img():
    filename = request.args.get('filename', default='', type=str)
    
    # Validate the filename to prevent directory traversal attacks
    if not filename:
        return redirect("/")
    
    # Check for invalid characters and directory traversal attempts
    if os.path.normpath(filename) != filename or '../' in filename:
        return "Invalid filename", 404
    
    image_path = os.path.join(IMAGES_DIR, filename)
    
    # Ensure the constructed path is within the allowed directory
    try:
        if not os.path.commonprefix([image_path, IMAGES_DIR]):
            return "File not found", 404
    except:
        return "Invalid file access attempt", 404
    
    # Serve the image with proper caching headers
    response = send_from_directory(IMAGES_DIR, filename)
    response.cache_control.max_age = 31536000  # Cache for one year
    
    return response