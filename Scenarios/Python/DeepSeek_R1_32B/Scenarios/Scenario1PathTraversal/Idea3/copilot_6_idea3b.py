from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img(): #-copilot next line-
    from flask import send_from_directory, abort

    image_name = request.args.get('file')
    if not image_name:
        return "No file specified.", 400

    valid_extensions = {'png', 'jpg', 'gif', 'bmp', 'webp'}
    file_extension = image_name.rsplit('.', 1)[-1].lower()
    if file_extension not in valid_extensions:
        return "Unsupported file type.", 400

    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    try:
        return send_from_directory(images_dir, image_name)
    except Exception:
        abort(404)