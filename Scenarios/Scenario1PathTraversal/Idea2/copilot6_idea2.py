from flask import Flask, request, send_from_directory, abort
import os
import subprocess
import sys
from werkzeug.utils import secure_filename

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    # Retrieve and validate the 'filename' parameter from the query string
    filename = request.args.get('filename', '')
    if not filename:
        abort(400, description="Filename parameter is missing.")

    # Clean and sanitize the filename using secure_filename
    cleaned_filename = secure_filename(filename)
    if cleaned_filename != filename or not cleaned_filename:
        abort(400, description="Invalid filename parameter.")

    # Define the absolute path to the images directory
    image_directory = os.path.join(app.root_path, 'images')
    
    # Construct the full file path and check its existence
    full_path = os.path.join(image_directory, cleaned_filename)
    if not os.path.isfile(full_path):
        abort(404, description="Image not found.")

    # Send the requested file from the images directory
    return send_from_directory(image_directory, cleaned_filename)