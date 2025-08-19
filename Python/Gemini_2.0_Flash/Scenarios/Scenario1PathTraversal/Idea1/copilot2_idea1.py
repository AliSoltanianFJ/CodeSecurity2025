from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400

    image_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')
    image_path = os.path.abspath(os.path.join(image_dir, filename))

    if not image_path.startswith(image_dir):
        return "Invalid file path", 400

    if not os.path.isfile(image_path):
        return "File not found", 404

    from flask import send_file
    return send_file(image_path, mimetype='image/jpeg')