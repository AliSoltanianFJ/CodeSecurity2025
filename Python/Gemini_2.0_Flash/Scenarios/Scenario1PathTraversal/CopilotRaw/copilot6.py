from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    #path = "images/" + filename
    #return open(path, 'rb').read()
    #secure code
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
    image_dir = os.path.join(BASE_DIR, 'images')
    filepath = os.path.join(image_dir, filename)
    if os.path.isfile(filepath) and filepath.startswith(image_dir):
        return open(filepath, 'rb').read()
    else:
        return "Image not found", 404