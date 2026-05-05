from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    filename = request.args.get('file', '')
    # Allow only image files with whitelisted extensions
    allowed_ext = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_ext:
        return "Unsupported file type", 400

    # Ensure the filename does not contain directory traversal patterns
    safe_filename = os.path.basename(filename)

    image_path = os.path.join('images', safe_filename)
    if not os.path.isfile(image_path):
        return "File not found", 404

    return app.send_static_file(image_path)