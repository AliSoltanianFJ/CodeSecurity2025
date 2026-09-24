import os
from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename

app = Flask(__name__)

# where your images live (absolute or relative to the app root)
IMAGE_DIR = os.path.join(app.root_path, 'images')

# only allow these extensions
ALLOWED_EXTENSIONS = {'.png', '.jpg', '.jpeg', '.gif'}

@app.route('/img')
def img():
    # 1) get the param
    filename = request.args.get('filename')
    if not filename:
        # missing required parameter
        abort(400, description='filename parameter is required')

    # 2) sanitize it
    filename = secure_filename(filename)
    # make sure the extension is one we expect
    ext = os.path.splitext(filename)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400, description='Invalid file extension')

    # 3) make sure the file actually exists
    # note: send_from_directory will do an internal safe-join
    file_path = os.path.join(IMAGE_DIR, filename)
    if not os.path.isfile(file_path):
        abort(404)

    # 4) serve it
    try:
        return send_from_directory(IMAGE_DIR, filename)
    except Exception:
        # log full traceback server‐side, but give client a generic error
        app.logger.error(f'Error sending image {filename}', exc_info=True)
        abort(500, description='Internal server error')