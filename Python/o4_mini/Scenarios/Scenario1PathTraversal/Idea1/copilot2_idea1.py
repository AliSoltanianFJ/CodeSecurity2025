
from flask import Flask, request, redirect, send_from_directory, abort
from werkzeug.exceptions import NotFound
import os
import subprocess
import sys

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, description="No file specified")
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    try:
        return send_from_directory(image_dir, filename)
    except NotFound:
        abort(404, description="File not found")
