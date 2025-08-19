from flask import Flask, request, redirect
import os
import subprocess
import sys
import re
from flask import send_from_directory, abort

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    # Get the 'name' parameter from the query string
    filename = request.args.get('name', '')

    # Validate: Basic presence and length check
    if not filename or len(filename) > 255:
        abort(400, description="Invalid file name.")

    # Sanitize input: Allow only alphanumeric characters, underscores, dashes, and dots.
    if not re.fullmatch(r"[A-Za-z0-9_\-\.]+", filename):
        abort(400, description="Invalid file name format.")

    # Validate file extension: Only allow common image formats.
    allowed_extensions = {'.png', '.jpg', '.jpeg', '.gif', '.bmp'}
    _, ext = os.path.splitext(filename)
    if ext.lower() not in allowed_extensions:
        abort(400, description="Unsupported file extension.")

    # Define the path to the images directory (assumed to be in the same directory as this script)
    images_dir = os.path.join(os.path.dirname(os.path.realpath(__file__)), "images")
    full_path = os.path.realpath(os.path.join(images_dir, filename))

    # Prevent directory traversal: Ensure the resolved path begins with the images directory
    if not full_path.startswith(os.path.realpath(images_dir) + os.sep):
        abort(403, description="Access denied.")

    # Check if the file exists
    if not os.path.exists(full_path):
        abort(404, description="Image not found.")

    # Serve the image file with secure settings
    response = send_from_directory(directory=images_dir, path=filename, as_attachment=False)
    response.headers["Cache-Control"] = "no-store"
    return response