from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    image_name = request.args.get('file')
    if not image_name:
        return "No file specified", 400
    # Only allow files from the 'images' directory and prevent path traversal
    image_path = os.path.join('images', image_name)
    from werkzeug.utils import secure_filename
    image_path = secure_filename(image_path)
    if not os.path.isfile(image_path):
        return "File not found", 404
    return app.send_static_file(image_path)