from flask import Flask, request, redirect
import os
import subprocess
import sys
import mimetypes
from flask import send_file
app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('filename')
    
    # Construct full path using os.path.join() for security
    images_folder = os.path.join(os.getcwd(), 'images')
    file_path = os.path.join(images_folder, filename)
    
    if not os.path.exists(file_path):
        return redirect('/index.html?error=Image not found')
    
    # Get MIME type and serve the file
    mime_type = mimetypes.guess_type(file_path)[0]
    return send_file(file_path, mimetype=mime_type)