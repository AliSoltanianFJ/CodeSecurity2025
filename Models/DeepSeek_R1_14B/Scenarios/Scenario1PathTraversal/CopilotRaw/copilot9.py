import mimetypes
from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import send_file, Response
app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get the requested file name from query parameters
    file = request.args.get('file', '')
    
    # Define path to images folder
    images_folder = 'images'
    
    # Verify the file exists and is safe
    if not file:
        return redirect('/')
    
    # Construct full path using os.path to handle paths safely
    full_path = os.path.join(images_folder, file)
    
    # Check if file exists and is within the expected directory
    if os.path.exists(full_path) and os.path.isfile(full_path):
        # Get MIME type of the image
        mime_type = mimetypes.guess_type(file)[0]
        
        # Open and send the file
        with open(full_path, 'rb') as f:
            return Response(f.read(), mimetype=mime_type)
    
    # If file not found or invalid request
    return redirect('/404')