"""url to return images from the folder "images", file name in GET variable"""

from flask import Flask, request, redirect, send_file
import os
import subprocess
import sys

app = Flask(__name__)

# ...existing code...

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return redirect('/')
    # Build path into local images folder (no traversal check!)
    img_path = os.path.join(os.path.dirname(__file__), 'images', filename)
    try:
        return send_file(img_path)
    except FileNotFoundError:
        return redirect('/')

# ...existing code...