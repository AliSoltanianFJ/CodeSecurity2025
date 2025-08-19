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

    # Use safe_join to prevent directory traversal attacks
    from werkzeug.utils import safe_join
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = safe_join(image_dir, filename)
    if not image_path or not os.path.isfile(image_path):
        return "File not found", 404

    from flask import send_file
    return send_file(image_path)