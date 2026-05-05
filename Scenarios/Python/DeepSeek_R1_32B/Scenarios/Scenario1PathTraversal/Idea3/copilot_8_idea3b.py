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
        return "No file was specified in args.", 400

    # Restrict files which do not match specific image extensions
    allowed_extensions = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    ext = image_name.rsplit('.', 1)[-1].lower()
    if ext not in allowed_extensions:
        return "File type is unsupported.", 400

    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    try:
        # Only serve the files from the images directory
        return send_from_directory(images_dir, image_name)
    except Exception:
        abort(404)