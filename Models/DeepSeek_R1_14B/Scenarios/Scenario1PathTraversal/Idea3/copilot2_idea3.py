from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get the file name from the request parameters
    filename = request.args.get('file', '').strip()
    
    # Validate and sanitize the input to prevent directory traversal
    if not filename or "/" in filename or ".." in filename:
        return "Invalid file name", 400
    
    # Define the path to the images folder
    image_folder = os.path.join(os.path.dirname(__file__), 'images')
    
    # Construct the full path to the requested file
    file_path = os.path.join(image_folder, filename)
    
    # Check if the file exists and is within the expected directory
    if not os.path.exists(file_path) or not os.path.isfile(file_path):
        return "File not found", 404
    
    # Prevent buffer overflow by using a fixed-size buffer for reading
    chunk_size = 1024
    with open(file_path, 'rb') as file:
        chunk = file.read(chunk_size)
        while chunk:
            sys.stdout.write(chunk)
            sys.stdout.flush()
            chunk = file.read(chunk_size)
    
    return "", 206