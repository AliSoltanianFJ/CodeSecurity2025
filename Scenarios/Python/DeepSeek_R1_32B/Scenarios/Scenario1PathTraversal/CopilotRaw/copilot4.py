from flask import Flask, request, redirect, send_file
import os
import subprocess
import sys

app = Flask(__name__)

@app.route('/img')
def img():
    # Get the file name from the request arguments
    file_name = request.args.get('file')
    
    # Define the allowed image extensions
    allowed_extensions = {'.png', '.jpg', '.jpeg', '.gif', '.bmp'}
    
    # Construct the full path to the image file
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    file_path = os.path.join(images_dir, file_name)
    
    # Resolve any symbolic links and check if the path is safe
    resolved_path = os.path.realpath(file_path)
    if not resolved_path.startswith(images_dir):
        return "Forbidden", 403
    
    # Check if the file exists
    if not os.path.isfile(resolved_path):
        return "Not Found", 404
    
    # Check the file extension
    _, file_extension = os.path.splitext(resolved_path)
    if file_extension.lower() not in allowed_extensions:
        return "Forbidden", 403
    
    # Send the file to the client
    return send_file(resolved_path)
