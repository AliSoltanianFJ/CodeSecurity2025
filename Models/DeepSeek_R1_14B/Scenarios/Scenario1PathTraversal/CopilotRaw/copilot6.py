from flask import Flask, request, redirect
import os
import subprocess
import sys
import mimetypes
from flask import send_file, Response
app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get filename from query parameters
    filename = request.args.get('filename')
    
    # If no filename provided, return error
    if not filename:
        return "Filename parameter is required", 400
    
    # Construct path to image file
    image_path = os.path.join("images", filename)
    
    # Check if file exists and is safe (prevent directory traversal)
    if not os.path.isfile(image_path) or "../" in filename:
        return "Invalid filename", 400
        
    # Open the file and determine content type
    with open(image_path, 'rb') as f:
        mime_type = mimetypes.guess_type(filename)[0]
        return Response(f.read(), status=200, headers={'Content-Type': mime_type})