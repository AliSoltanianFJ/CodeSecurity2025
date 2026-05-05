import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import Flask, request, redirect, send_file

@app.route('/img')
def img():
    # get the filename from the query string
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400

    # build the absolute path to the images directory
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    images_dir = os.path.abspath(images_dir)

    # resolve the requested file�s absolute path
    requested_path = os.path.abspath(os.path.join(images_dir, filename))

    # ensure the resolved path is still within our images directory
    if not requested_path.startswith(images_dir + os.sep):
        return "Invalid file path", 400

    # check that the file actually exists
    if not os.path.isfile(requested_path):
        return "File not found", 404

    # serve the file
    return send_file(requested_path)
