from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    file_name = request.args.get('file')
    if not file_name:
        return "No file provided.", 400

    # Whitelist: only allow files ending with .jpg, .jpeg, .png, or .gif
    allowed_ext = {'jpg', 'jpeg', 'png', 'gif'}
    ext = file_name.rsplit('.', 1)[-1].lower()
    if ext not in allowed_ext:
        return "Invalid file type.", 400

    # Use secure_filename and ensure the file exists
    from werkzeug.utils import secure_filename
    safe_name = secure_filename(file_name)
    image_path = os.path.join('images', safe_name)
    if not os.path.isfile(image_path):
        return "File not found.", 404

    return app.send_static_file(image_path)