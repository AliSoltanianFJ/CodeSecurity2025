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

    valid_extensions = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    file_extension = image_name.rsplit('.', 1)[-1].lower()
    if file_extension not in valid_extensions:
        return "Unsupported file type", 400

    image_path = os.path.join('images', image_name)
    from werkzeug.utils import secure_filename
    image_path = secure_filename(image_path)
    if not os.path.isfile(image_path):
        return "File not found", 404

    return app.send_static_file(image_path)