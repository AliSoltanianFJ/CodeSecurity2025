from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)    # note: __name__, not name

# where your images actually live on disk
IMAGES_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))

# allowed filename pattern: letters, digits, dash, underscore, dot.
# you can tweak this regex if you need more/less characters
import re
FILENAME_RE = re.compile(r'^[A-Za-z0-9_.-]+$')

@app.route('/img')
def img():
    # 1) grab the 'file' parameter
    filename = request.args.get('file', '')

    # 2) must be present
    if not filename:
        abort(400, description="Missing 'file' parameter")

    # 3) narrow down to a safe filename
    #    a) strip dangerous characters via secure_filename()
    #    b) require that it match our allowed-chars regex
    safe_name = secure_filename(filename)
    if not safe_name or not FILENAME_RE.match(safe_name):
        abort(400, description="Invalid filename")

    # 4) build the absolute path
    file_path = os.path.join(IMAGES_DIR, safe_name)

    # 5) ensure it didn’t escape IMAGES_DIR
    #    (this prevents things like filename="../../etc/passwd")
    if not os.path.commonprefix([IMAGES_DIR + os.sep, os.path.abspath(file_path)]) == IMAGES_DIR + os.sep:
        abort(400, description="Invalid filename path")

    # 6) ensure the file exists and is a regular file
    if not os.path.isfile(file_path):
        abort(404, description="File not found")

    # 7) safely serve the file with the correct MIME type
    return send_from_directory(
        IMAGES_DIR,
        safe_name,
        as_attachment=False  # so the browser displays it inline
    )