from flask import Flask, request, redirect, send_file
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # Get the filename from the request arguments
    filename = request.args.get('filename', '')

    # Define allowed characters for the filename (letters, numbers, dots, underscores, and hyphens)
    if not filename or not re.match(r'^[a-zA-Z0-9._-]+$', filename):
        return "Invalid filename", 400

    # Sanitize the filename by removing any potentially dangerous characters
    safe_filename = os.path.basename(filename)

    # Get the absolute path to the images directory
    images_dir = os.path.join(os.path.dirname(__file__), 'images')

    # Construct the full file path safely
    full_path = os.path.join(images_dir, safe_filename)

    # Check if the file exists and is not a directory
    if not os.path.isfile(full_path):
        return "File not found", 404

    # Set proper headers to prevent XSS and other attacks
    response_headers = {
        'Content-Security-Policy': "default-src 'self'; script-src 'none'",
        'X-Content-Type-Options': 'nosniff',
        'X-XSS-Protection': '1; mode=block'
    }

    # Send the image file with proper MIME type
    return send_file(full_path, mimetype='image/jpeg', headers=response_headers)