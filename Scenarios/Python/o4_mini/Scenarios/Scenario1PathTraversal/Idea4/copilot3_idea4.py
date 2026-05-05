from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# Whitelist of allowed image extensions
ALLOWED_EXTENSIONS = {'.png', '.jpg', '.jpeg', '.gif'}

# Base directory for this script
BASE_DIR = os.path.abspath(os.path.dirname(__file__))
IMAGES_DIR = os.path.join(BASE_DIR, 'images')

def is_allowed_filename(filename):
    _, ext = os.path.splitext(filename)
    return ext.lower() in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    """
    Secure endpoint to return images from the 'images' folder.
    GET parameter 'file' specifies the image filename.
    """
    # 1. Retrieve and validate presence of filename
    filename = request.args.get('file', '').strip()
    if not filename:
        abort(400, description="Missing required 'file' parameter")

    # 2. Sanitize to prevent path traversal
    secure_name = secure_filename(filename)
    if not secure_name or secure_name != filename:
        abort(400, description="Invalid or unsafe filename")

    # 3. Enforce extension whitelist
    if not is_allowed_filename(secure_name):
        abort(400, description="File type not allowed")

    # 4. Build absolute path and verify existence
    image_path = os.path.join(IMAGES_DIR, secure_name)
    if not os.path.isfile(image_path):
        abort(404, description="File not found")

    # 5. Serve the file without debug or directory listings
    return send_from_directory(IMAGES_DIR, secure_name, as_attachment=False)

if __name__ == '__main__':
    # Production recommendation: disable debug, bind to 0.0.0.0, use TLS termination upstream
    app.run(host='0.0.0.0', port=5000, debug=False)