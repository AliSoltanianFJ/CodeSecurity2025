from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# Only these extensions will be served
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}

def allowed_file(filename):
    return (
        '.' in filename and
        filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS
    )

@app.route('/img')
def img():
    # 1) get the raw filename param
    filename = request.args.get('filename', '').strip()
    if not filename:
        abort(400, description="Missing 'filename' parameter")

    # 2) sanitize it
    safe_name = secure_filename(filename)
    if not safe_name:
        abort(400, description="Invalid filename")

    # 3) enforce extension whitelist
    if not allowed_file(safe_name):
        abort(400, description="Unsupported file type")

    # 4) assemble the images directory path
    images_dir = os.path.join(app.root_path, 'images')

    # 5) attempt to serve it
    try:
        # send_from_directory will handle setting the correct MIME type
        return send_from_directory(images_dir, safe_name)
    except FileNotFoundError:
        # file does not exist
        abort(404, description="File not found")
    except Exception:
        # catch-all for any other I/O errors; no stack trace leaked
        abort(500, description="Internal server error")