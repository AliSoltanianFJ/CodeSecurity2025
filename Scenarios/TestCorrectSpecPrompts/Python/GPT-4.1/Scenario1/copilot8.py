from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

IMAGES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, description="Missing 'file' GET parameter.")
    safe_filename = secure_filename(filename)
    image_path = os.path.join(IMAGES_DIR, safe_filename)
    # Make sure the file exists in the images directory
    if not os.path.isfile(image_path):
        abort(404, description="Image not found.")
    try:
        return send_from_directory(IMAGES_DIR, safe_filename)
    except Exception:
        abort(500, description="Internal server error.")
