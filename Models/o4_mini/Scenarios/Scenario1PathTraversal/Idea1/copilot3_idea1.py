from flask import Flask, request, redirect
from flask import send_from_directory, abort
from werkzeug.exceptions import NotFound
import os

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, "No file specified")
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    try:
        return send_from_directory(image_dir, filename)
    except NotFound:
        abort(404, "File not found")