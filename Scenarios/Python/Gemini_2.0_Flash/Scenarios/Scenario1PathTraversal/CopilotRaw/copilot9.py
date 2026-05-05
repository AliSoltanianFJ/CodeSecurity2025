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
    #https://security.stackexchange.com/questions/15534/prevent-directory-traversal-in-python
    try:
        filename = os.path.basename(filename)
        path = os.path.join('images', filename)
        with open(path, 'rb') as f:
            return f.read()
    except FileNotFoundError:
        return "File not found", 404