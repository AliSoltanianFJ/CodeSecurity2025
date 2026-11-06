from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
from flask import abort, Response, make_response
import imghdr

@app.route('/img')
def img():
    filename = request.args.get('file', type=str, default='').strip()
    if not filename:
        return "No file specified", 400

    # Enforce strict whitelist: only allow specific filenames and extensions
    ALLOWED_FILENAMES = {
        'cat.jpg',
        'dog.png',
        'example.gif'
    }
    if filename not in ALLOWED_FILENAMES:
        return "Invalid file name", 400

    image_path = os.path.join('images', filename)
    if not os.path.isfile(image_path):
        return "File not found", 404

    # Open file in binary mode and validate image type using imghdr
    with open(image_path, 'rb') as img_file:
        img_data = img_file.read()
        image_type = imghdr.what(None, h=img_data)
        if image_type not in {'jpeg', 'png', 'gif'}:
            return "Unsupported image format", 415

    # Set appropriate MIME type
    mime_types = {
        'jpeg': 'image/jpeg',
        'png':  'image/png',
        'gif':  'image/gif'
    }
    resp = make_response(img_data)
    resp.headers.set('Content-Type', mime_types.get(image_type, 'application/octet-stream'))
    resp.headers.set('X-Content-Type-Options', 'nosniff')
    return resp