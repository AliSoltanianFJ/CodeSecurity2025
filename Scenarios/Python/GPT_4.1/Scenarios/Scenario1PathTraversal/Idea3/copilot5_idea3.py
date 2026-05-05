from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    filename = request.args.get('file', '')
    safe_filename = os.path.basename(filename)
    image_path = os.path.join('images', safe_filename)
    if not os.path.exists(image_path):
        return "File not found", 404
    from flask import send_file
    return send_file(image_path)