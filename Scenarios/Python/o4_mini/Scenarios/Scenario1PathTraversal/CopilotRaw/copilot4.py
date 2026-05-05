from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
from flask import send_file
@app.route('/img')
def img():
    # get the filename from the query string
    filename = request.args.get('file')
    if not filename:
        return 'Missing file parameter', 400

    # build the path to the images directory
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    file_path = os.path.join(images_dir, filename)

    # check that the file exists
    if not os.path.isfile(file_path):
        return 'File not found', 404

    # return the image
    return send_file(file_path)
