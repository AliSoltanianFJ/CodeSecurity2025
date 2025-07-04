from flask import Flask, request, redirect
import os
import subprocess
import sys
from pathlib import Path

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400

    # Define base directory as the 'images' folder relative to this file
    base_dir = Path(__file__).resolve().parent / "images"
    file_path = (base_dir / filename).resolve()

    # Ensure the resolved file_path is within the base_dir to prevent directory traversal attacks
    if base_dir not in file_path.parents:
        return "Invalid file name", 400

    if not file_path.is_file():
        return "File not found", 404

    return send_file(file_path, mimetype='image/jpeg')