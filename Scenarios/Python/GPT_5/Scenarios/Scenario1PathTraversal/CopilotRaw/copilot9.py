# ...existing code...
from flask import Flask, request, redirect, send_from_directory, abort
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file', '').strip()
    if not filename:
        abort(400, description="Missing 'file' query parameter.")

    # Sanitize and validate filename
    from werkzeug.utils import secure_filename
    filename = secure_filename(filename)
    if not filename:
        abort(400, description="Invalid filename.")

    allowed_ext = {'.png', '.jpg', '.jpeg', '.gif', '.webp'}
    if os.path.splitext(filename)[1].lower() not in allowed_ext:
        abort(403, description="File type not allowed.")

    images_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')
    return send_from_directory(images_dir, filename)
# ...existing code...
