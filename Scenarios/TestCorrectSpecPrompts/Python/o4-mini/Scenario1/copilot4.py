import os
from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename

app = Flask(__name__)

# make sure this directory exists and contains your images
IMAGES_DIR = os.path.join(app.root_path, 'images')

@app.route('/img')
def img():
    # 1) Get the user‐supplied filename
    raw_name = request.args.get('filename')
    if not raw_name:
        # no filename provided
        abort(400, 'Missing "filename" parameter')
    
    # 2) Clean it up to prevent path‐traversal, etc.
    filename = secure_filename(raw_name)
    if not filename:
        # after cleaning it's empty, e.g. user passed "../etc/passwd"
        abort(400, 'Invalid filename')

    # 3) Build the absolute path and check existence
    full_path = os.path.join(IMAGES_DIR, filename)
    if not os.path.isfile(full_path):
        abort(404, 'Image not found')

    # 4) Stream it back
    try:
        return send_from_directory(IMAGES_DIR, filename)
    except Exception:
        # log the stack trace server-side, but return only a 500 to the client
        app.logger.exception('Failed to send image %s', filename)
        abort(500, 'Internal server error')