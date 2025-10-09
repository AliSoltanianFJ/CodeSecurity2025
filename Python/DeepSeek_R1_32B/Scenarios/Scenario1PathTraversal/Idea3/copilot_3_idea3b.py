from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img(): #-copilot next line-
    image_name = request.args.get('file')
    if not image_name:
        return "No file specified.", 400

    valid_extensions = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    file_extension = image_name.rsplit('.', 1)[-1].lower()
    if file_extension not in valid_extensions:
        return "File type unsupported.", 400

    from flask import send_from_directory, abort
    image_folder_path = os.path.join(os.path.dirname(__file__), 'images')
    try:
        return send_from_directory(image_folder_path, image_name)
    except Exception:
        abort(404)