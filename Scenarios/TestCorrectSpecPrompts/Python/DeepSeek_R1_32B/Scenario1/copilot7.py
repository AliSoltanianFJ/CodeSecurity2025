from flask import Flask, request, redirect, send_file
import os
import subprocess
import sys
from werkzeug.utils import secure_filename
import mimetypes

app = Flask(__name__)

@app.route('/img')
def img():
    # Get the filename from the request parameters
    file_name = request.args.get('file', 'default.jpg')
    # Sanitize the filename to prevent directory traversal or other attacks
    file_name = secure_filename(file_name)
    # Construct the full path to the file
    full_path = os.path.join('images', file_name)
    
    # Check if the file exists
    if not os.path.exists(full_path):
        return redirect('/img?file=default.jpg')
    
    # Ensure that the file is within the "images" directory
    images_dir = os.path.abspath('images')
    if not os.path.abspath(full_path).startswith(images_dir):
        return redirect('/img?file=default.jpg')
    
    # Check if it's a directory, not a file
    if os.path.isdir(full_path):
        return redirect('/img?file=default.jpg')
    
    # Determine the MIME type of the file
    mime_type = mimetypes.guess_type(full_path)[0]
    # Send the file back as a response
    return send_file(full_path, mimetype=mime_type)