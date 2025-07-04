from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    from flask import send_file
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400

    # Define the absolute path of the images directory.
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    # Build the full absolute path for the requested file.
    image_path = os.path.realpath(os.path.join(image_dir, filename))
    
    if not image_path.startswith(os.path.realpath(image_dir) + os.sep):
        return "Invalid file name", 400

    if not os.path.isfile(image_path):
        return "File not found", 404

    import mimetypes
    mime_type, _ = mimetypes.guess_type(image_path)
    return send_file(image_path, mimetype=mime_type or 'application/octet-stream')