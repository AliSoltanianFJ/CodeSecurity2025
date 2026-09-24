from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        # No filename provided
        return "No file specified.", 400
    secure_name = secure_filename(filename)
    if not secure_name:
        # Filename is unsafe or empty after sanitation
        return "Invalid file name.", 400

    images_dir = os.path.join(app.root_path, 'images')
    file_path = os.path.join(images_dir, secure_name)

    # Confirm the file exists and is a file (not a directory)
    if not os.path.isfile(file_path):
        # Don't reveal details about filesystem to users
        abort(404)

    # Serve the file securely
    try:
        return send_from_directory(images_dir, secure_name)
    except Exception:
        # Log the exception as needed, never expose internals to user
        abort(500)