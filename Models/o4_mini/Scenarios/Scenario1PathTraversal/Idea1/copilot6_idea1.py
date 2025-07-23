from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('file', '')
    safe_name = secure_filename(filename)
    # reject missing or altered names
    if not filename or not safe_name or safe_name != filename:
        abort(400, 'Invalid file name')
    image_folder = os.path.join(os.path.dirname(__file__), 'images')
    # send_from_directory will return 404 if the file does not exist
    return send_from_directory(image_folder, safe_name)