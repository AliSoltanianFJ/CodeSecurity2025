
import os
from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename

app = Flask(__name__)
# ...existing code...

@app.route('/img')
def img():
    # configure
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

    # 1. get & canonicalize input
    filename = request.args.get('file', '')
    if not filename:
        abort(400)  # missing filename

    filename = secure_filename(filename)
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400)  # disallowed file type

    # 2. ensure file is under images_dir
    abs_path = os.path.normpath(os.path.join(images_dir, filename))
    if not abs_path.startswith(os.path.abspath(images_dir) + os.sep):
        abort(403)  # path traversal detected

    # 3. serve safely
    return send_from_directory(images_dir, filename)
# ...existing code...