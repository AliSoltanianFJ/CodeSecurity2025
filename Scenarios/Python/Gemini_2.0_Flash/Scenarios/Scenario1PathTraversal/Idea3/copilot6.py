from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
def img():
    from flask import send_from_directory, abort

    filename = request.args.get('name')
    if not filename:
        return "No filename was provided.", 400

    valid_extensions = {'PNG', 'JPG', 'JPEG', 'GIF', 'BMP', 'WEBP'}
    file_extension = filename.rsplit('.', 1)[-1].upper()
    if file_extension not in valid_extensions:
        return "File type is not supported.", 400

    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    try:
        return send_from_directory(images_dir, filename)
    except Exception:
        abort(404)