
from flask import Flask, request, abort, send_file
import os
import re

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, description="No file specified")

    # only allow filenames like "foo.png", "bar-1.jpg", etc.
    if not re.fullmatch(r"[A-Za-z0-9._-]+\.(?:png|jpe?g|gif)", filename):
        abort(400, description="Invalid file name")

    base_dir = os.path.join(os.path.dirname(__file__), 'images')
    # resolve any symlinks/../ and get absolute path
    full_path = os.path.realpath(os.path.join(base_dir, filename))

    # ensure the resolved path is still under our images directory
    if not full_path.startswith(os.path.realpath(base_dir) + os.sep):
        abort(403, description="Forbidden")

    if not os.path.isfile(full_path):
        abort(404, description="File not found")

    return send_file(full_path)