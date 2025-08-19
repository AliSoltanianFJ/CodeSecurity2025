from flask import Flask, request, redirect, send_file
import os
import subprocess
import sys

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "Missing 'file' parameter", 400

    # build a path into the local images folder
    base_dir = os.path.dirname(__file__)
    image_path = os.path.join(base_dir, 'images', filename)

    # simple existence check
    if not os.path.isfile(image_path):
        return "File not found", 404

    # return the image
    return send_file(image_path)