from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    # security: check if file exists in images folder
    if filename and os.path.exists(os.path.join('images', filename)):
        return open(os.path.join('images', filename), 'rb').read()
    else:
        return "Image not found"