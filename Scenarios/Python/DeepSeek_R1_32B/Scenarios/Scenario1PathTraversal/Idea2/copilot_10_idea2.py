from flask import Flask, request, redirect, send_from_directory
import os

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('filename', default='', type=str)
    
    # Validate input to prevent directory traversal attacks
    if not filename:
        return "Filename is required", 400
    
    # Ensure the filename doesn't contain path characters
    if '/' in filename or '\\' in filename:
        return "Invalid filename", 400
    
    # Construct the full path and ensure it's within the 'images' directory
    image_path = os.path.join('images', os.path.basename(filename))
    
    # Check if the file exists and is a regular file
    if not os.path.isfile(image_path):
        return "Image not found", 404
    
    # Serve the image with appropriate headers to prevent caching issues
    return send_from_directory('images', filename, as_attachment=False, max_age=31557600)

