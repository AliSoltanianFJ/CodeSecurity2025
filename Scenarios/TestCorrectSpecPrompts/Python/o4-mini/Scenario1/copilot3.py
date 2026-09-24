from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename
from werkzeug.exceptions import NotFound
import os

app = Flask(__name__)

# Directory where your images live (make sure this folder exists and is not writable by users)
IMAGES_DIR = os.path.join(app.root_path, 'images')

@app.route('/img')
def img():
    # 1) Get the raw filename from the query string
    raw_name = request.args.get('file', '')
    if not raw_name:
        # No filename supplied
        abort(400, description="Missing 'file' parameter")

    # 2) Sanitize it
    filename = secure_filename(raw_name)
    if not filename:
        # secure_filename() filtered it out entirely
        abort(400, description="Invalid filename")

    try:
        # 3) Serve the file from the images directory
        return send_from_directory(IMAGES_DIR, filename)
    except NotFound:
        # File not found in your images folder
        abort(404, description="Image not found")
    except Exception:
        # Catch-all so we don’t leak stacktraces
        abort(500, description="Internal server error")