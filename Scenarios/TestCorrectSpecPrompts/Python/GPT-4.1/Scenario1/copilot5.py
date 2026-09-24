from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

IMAGE_FOLDER = os.path.join(os.path.dirname(__file__), 'images')

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, "No filename provided")
    safe_filename = secure_filename(filename)
    file_path = os.path.join(IMAGE_FOLDER, safe_filename)
    # Ensure that the file exists in the images directory
    if not os.path.exists(file_path):
        abort(404, "File not found")
    # Only allow files from the intended folder
    return send_from_directory(IMAGE_FOLDER, safe_filename)

# example usage: /img?file=picture.png