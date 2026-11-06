import os
import subprocess
import sys
import re
from flask import Flask, request, abort, send_from_directory

app = Flask(__name__)

# Maximum acceptable file size on disk (optional, in bytes)
# app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MiB

# Only allow filenames of the form [A-Z,a-z,0-9,underscore,hyphen]+ . extension
_FILENAME_REGEX = re.compile(r'^[A-Za-z0-9_-]+\.(?:png|jpe?g|gif|bmp)$')

@app.route('/img')
def img():
    # 1) Get the ‘file’ parameter
    filename = request.args.get('file')
    if not filename:
        # no file specified
        abort(400, description='No file specified')

    # 2) Validate filename against strict regex (whitelisting)
    if not _FILENAME_REGEX.fullmatch(filename):
        # disallow path traversal, weird characters, hidden files, etc.
        abort(400, description='Invalid file name')

    # 3) Resolve the absolute path of the images directory
    base_dir = os.path.dirname(os.path.abspath(__file__))
    images_dir = os.path.realpath(os.path.join(base_dir, 'images'))

    # 4) Resolve the requested file’s absolute path
    requested_path = os.path.realpath(os.path.join(images_dir, filename))

    # 5) Ensure the resolved path is indeed inside images_dir
    #    (this catches ../ symlink tricks, etc.)
    if not requested_path.startswith(images_dir + os.sep):
        abort(400, description='Invalid file name')

    # 6) Ensure the file exists and is a regular file
    if not os.path.isfile(requested_path):
        abort(404, description='File not found')

    # 7) Safely serve the file
    #    send_from_directory will set the Content-Type based on extension
    return send_from_directory(images_dir, filename)
