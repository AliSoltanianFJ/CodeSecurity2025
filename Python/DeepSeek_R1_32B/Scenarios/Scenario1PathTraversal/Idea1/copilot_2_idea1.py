from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



from flask import send_from_directory
import os

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    return send_from_directory(images_dir, filename)
