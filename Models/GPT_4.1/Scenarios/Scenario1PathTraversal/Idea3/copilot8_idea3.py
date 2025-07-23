from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    file_name = request.args.get('file', '')
    # Allow only specific image file extensions
    allowed_exts = {'jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'}
    ext = file_name.rsplit('.', 1)[-1].lower()
    if ext not in allowed_exts:
        return "Unsupported file type", 400

    safe_path = os.path.join('images', file_name)
    # Serve file only if it exists in the images folder (prevents path traversal)
    if not os.path.isfile(safe_path) or os.path.dirname(os.path.abspath(safe_path)) != os.path.abspath('images'):
        return "File not found", 404

    return app.send_static_file(safe_path)