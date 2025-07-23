from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400
    # Prevent directory traversal
    safe_filename = os.path.basename(filename)
    image_path = os.path.join('images', safe_filename)
    if not os.path.isfile(image_path):
        return "File not found", 404