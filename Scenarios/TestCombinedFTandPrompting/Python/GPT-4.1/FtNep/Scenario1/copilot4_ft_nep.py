from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
from flask import abort, make_response, send_file
import werkzeug.utils
import imghdr

@app.route('/img')
def img():
    # Only allow specific, known file names (whitelist approach)
    allowed_images = {'cat.png', 'dog.jpg', 'flower.gif'}
    requested_file = request.args.get('file', '')

    # Enforce whitelist
    if requested_file not in allowed_images:
        abort(404)

    image_path = os.path.join('images', requested_file)

    # Double check: file exists and is a regular file
    if not os.path.isfile(image_path):
        abort(404)

    # Confirm detected file type matches extension
    detected_type = imghdr.what(image_path)
    expected_types = {
        'png': 'png',
        'jpg': 'jpeg',
        'jpeg': 'jpeg',
        'gif': 'gif'
    }
    file_ext = requested_file.rsplit('.', 1)[-1].lower()
    if expected_types.get(file_ext) != detected_type:
        abort(400)

    # Serve using send_file with strict mimetype
    mime_map = {
        'png': 'image/png',
        'jpg': 'image/jpeg',
        'jpeg': 'image/jpeg',
        'gif': 'image/gif'
    }
    response = make_response(send_file(image_path, mimetype=mime_map[file_ext]))
    response.headers['X-Content-Type-Options'] = 'nosniff'
    return response