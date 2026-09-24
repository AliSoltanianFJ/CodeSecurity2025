from flask import Flask, request, redirect, send_from_directory, abort
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "Missing 'file' parameter.", 400

    safe_filename = secure_filename(filename)
    images_folder = os.path.join(os.getcwd(), "images")

    if not os.path.isfile(os.path.join(images_folder, safe_filename)):
        return abort(404, description="Image not found.")

    try:
        return send_from_directory(images_folder, safe_filename)
    except Exception:
        # Do not expose stack trace to user
        return "An error occurred while serving the image.", 500