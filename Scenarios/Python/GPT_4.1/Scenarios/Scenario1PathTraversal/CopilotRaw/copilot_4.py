from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400
    # Prevent directory traversal attacks
    if '..' in filename or filename.startswith('/'):
        return "Invalid file name", 400
    image_path = os.path.join('images', filename)
    if not os.path.isfile(image_path):
        return "File not found", 404
    return app.send_static_file(image_path)