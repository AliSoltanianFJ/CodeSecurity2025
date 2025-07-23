from flask import Flask, request, redirect, send_from_directory
import os

app = Flask(__name__)

ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def is_allowed(filename):
    # Check extension
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # Validate input exists.
    filename = request.args.get('filename', '')
    if not filename:
        return "Missing filename parameter", 400

    # Clean and sanitize input - use os.path.basename to strip directory info.
    safe_filename = os.path.basename(filename)
    if safe_filename != filename:
        # If the filename was not the same, there is an attempt of directory traversal.
        return "Invalid filename", 400

    # Validate file extension.
    if not is_allowed(safe_filename):
        return "Invalid file type", 400

    # Define path to the images folder relative to current file.
    images_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')

    # Construct the full file path.
    file_path = os.path.join(images_dir, safe_filename)
    # Ensure the computed absolute file path is within the images directory.
    if not os.path.abspath(file_path).startswith(os.path.abspath(images_dir)):
        return "Invalid file path", 400

    # Check if file exists.
    if not os.path.exists(file_path):
        return "File not found", 404

    # Use Flask's send_from_directory for safe file serving.
    return send_from_directory(images_dir, safe_filename)