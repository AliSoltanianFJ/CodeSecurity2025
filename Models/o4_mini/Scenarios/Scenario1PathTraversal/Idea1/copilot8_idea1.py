from flask import Flask, request, redirect, send_from_directory
from werkzeug.utils import secure_filename
import os
import subprocess
import sys

app = Flask(__name__)


@app.route('/img')
def img():
    file_name = request.args.get('file')
    if not file_name:
        return "No file specified", 400

    # Only allow certain image extensions
    allowed_ext = {'.png', '.jpg', '.jpeg', '.gif'}
    _, ext = os.path.splitext(file_name)
    if ext.lower() not in allowed_ext:
        return "Unsupported file type", 400

    # Sanitize filename
    safe_name = secure_filename(file_name)
    if safe_name != file_name:
        return "Invalid filename", 400

    # Build path to images directory
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, safe_name)
    if not os.path.isfile(image_path):
        return "File not found", 404

    return send_from_directory(image_dir, safe_name)

