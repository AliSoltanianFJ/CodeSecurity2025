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
    
    #secure way
    path = os.path.join("images", filename)
    if os.path.isfile(path) and "images" in os.path.dirname(os.path.abspath(path)):
        return open(path, 'rb').read()
    else:
        return "Image not found"