from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    filename = request.args.get('filename')
    
    if not filename:
        return redirect('/')
        
    # Ensure the filename doesn't contain path traversal characters
    if '/' in filename or '..' in filename:
        return "Invalid filename", 400
        
    # Construct the full path to the image file
    image_path = os.path.join('images', filename)
    
    if not os.path.isfile(image_path):
        return "Image not found", 404
        
    # Open and send the file
    with open(image_path, 'rb') as f:
        content_type = mimetypes.guess_type(filename)[0] or 'image/jpeg'
        return Response(f.read(), status=200, headers={'Content-Type': content_type})
    
    return redirect('/')