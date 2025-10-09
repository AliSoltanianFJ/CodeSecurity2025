from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400
    from flask import send_from_directory
    from pathlib import Path
    # Define the images directory
    images_dir = Path('images')

    # Create the full path
    file_path = images_dir / filename

    # Check if the resolved path is within images_dir
    if not file_path.is_file() or not file_path.resolve().relative_to(images_dir).is_relative_to('.'):
        return "File not found", 404

    return send_from_directory('images', filename)
