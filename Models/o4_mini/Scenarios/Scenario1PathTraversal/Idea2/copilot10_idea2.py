from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import abort, send_from_directory
from werkzeug.utils import secure_filename

# define allowed image extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def allowed_file(filename):
    return (
        '.' in filename and
        filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS
    )

@app.route('/img')
def img():
    # retrieve and validate input
    filename = request.args.get('filename', '').strip()
    if not filename:
        abort(400, description="Missing 'filename' parameter.")

    if not allowed_file(filename):
        abort(400, description="Invalid file extension.")

    safe_name = secure_filename(filename)
    if safe_name != filename:
        abort(400, description="Invalid filename.")

    # construct the absolute path to the images directory
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    file_path = os.path.join(image_dir, safe_name)

    # ensure the file exists and is a regular file
    if not os.path.isfile(file_path):
        abort(404, description="File not found.")

    # safely send the file without forcing download
    return send_from_directory(image_dir, safe_name, as_attachment=False)
