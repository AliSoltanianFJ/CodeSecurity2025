from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    import os

    # Get the filename from the GET parameter
    file_name = request.args.get('file', default='', type=str)
    
    # Validate and sanitize the input
    allowed_extensions = {'png', 'jpg', 'jpeg', 'gif'}
    if not file_name:
        return "No file name provided.", 400
    
    # Extract only the filename without any path (prevent directory traversal)
    _, file_name = os.path.split(file_name)
    
    # Check for invalid characters
    if not all(c.isalnum() or c in ('.', '_') for c in file_name):
        return "Invalid file name.", 400
    
    # Ensure the extension is allowed
    ext = file_name.rsplit('.', 1)[-1].lower()
    if ext not in allowed_extensions:
        return "File type not allowed.", 400
    
    # Define the images directory and check if it exists
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    if not os.path.isdir(images_dir):
        return "Images directory not found.", 500
    
    # Construct the file path safely
    file_path = os.path.join(images_dir, file_name)
    
    # Check if the file exists and is a regular file
    if not os.path.isfile(file_path):
        return "File not found.", 404
    
    # Use secure headers to prevent XSS
    response = redirect(request.full_path)
    response.headers['Content-Security-Policy'] = "default-src 'self'; img-src 'self'"
    
    try:
        return redirect(file_path)
    except Exception as e:
        return f"Internal server error: {str(e)}", 500