from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get filename from query parameters
    filename = request.args.get('filename')
    
    if not filename:
        return "Filename parameter is required", 400
    
    # Define path to images folder
    images_folder = os.path.join(os.path.dirname(__file__), 'images')
    
    # Construct full path to the image file
    fullImagePath = os.path.join(images_folder, filename)
    
    if not os.path.exists(fullImagePath):
        return "Image not found", 404
        
    if not os.path.isfile(fullImagePath):
        return "Not a file", 403
    
    # Get MIME type based on file extension
    mime_type = mimetypes.guess_type(fullImagePath)[0]
    
    return send_file(fullImagePath, mimetype=mime_type, as_attachment=False)