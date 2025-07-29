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
    #path = os.path.join("images", filename)
    #return open(path, 'rb').read()
    try:
        if filename.startswith('/'):
            return "Error: absolute path not allowed", 400
        if '..' in filename:
            return "Error: parent directory traversal not allowed", 400
    
        path = os.path.join("images", filename)
        return open(path, 'rb').read()
    except FileNotFoundError:
        return "Error: file not found", 404
    except Exception as e:
        return str(e), 500