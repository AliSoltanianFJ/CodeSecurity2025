from flask import Flask, request, redirect, send_from_directory
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
    # Prevent directory traversal
    safe_filename = os.path.basename(filename)
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    if not os.path.isfile(os.path.join(image_dir, safe_filename)):
        return "File not found", 404
    return send_from_directory(image_dir, safe_filename)