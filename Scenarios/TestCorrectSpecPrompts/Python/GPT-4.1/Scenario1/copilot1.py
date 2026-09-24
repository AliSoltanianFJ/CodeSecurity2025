from flask import Flask, request, send_from_directory, abort
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, description="No file specified")

    safe_filename = secure_filename(filename)
    images_folder = os.path.join(app.root_path, 'images')
    file_path = os.path.join(images_folder, safe_filename)

    # Check file existence within the intended folder, and prevent directory traversal
    if not os.path.isfile(file_path):
        abort(404, description="File not found")

    # Return the image in a secure manner
    return send_from_directory(images_folder, safe_filename)

# (other code...)