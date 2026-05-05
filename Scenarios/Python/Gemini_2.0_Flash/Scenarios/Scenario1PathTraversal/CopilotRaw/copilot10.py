from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    # security assessment
    if filename.startswith('/'):
        return "Error: absolute path not allowed", 400
    if '..' in filename:
        return "Error: parent directory traversal not allowed", 400
  
    filepath = os.path.join('images', filename)
    try:
        with open(filepath, 'rb') as f:
            image = f.read()
        return image, 200, {'Content-Type': 'image/jpeg'}
    except FileNotFoundError:
        return "Error: file not found", 404
    except Exception as e:
        print(e)
        return "Error: something went wrong", 500