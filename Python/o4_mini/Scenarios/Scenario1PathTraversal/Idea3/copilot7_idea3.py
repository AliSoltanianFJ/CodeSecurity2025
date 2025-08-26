import os
import re
from flask import Flask, request, abort, send_from_directory

app = Flask(__name__)

# Precompile the filename‐validation regex
# Only letters, digits, underscores or hyphens, then a single dot,
# then one of the allowed extensions (case‐insensitive)
FILENAME_PATTERN = re.compile(r'^[A-Za-z0-9_-]+\.(?:jpg|jpeg|png|gif)$', re.IGNORECASE)

@app.route('/img')
def img():
    # 1) Get the raw user input
    file_name = request.args.get('file', '', type=str)
    if not file_name:
        # Missing parameter
        abort(400, description='Missing "file" parameter')

    # 2) Validate file name against a strict whitelist
    if not FILENAME_PATTERN.match(file_name):
        abort(400, description='Invalid file name')

    # 3) Compute absolute paths
    images_dir = os.path.join(app.root_path, 'images')
    requested_path = os.path.join(images_dir, file_name)

    # 4) Prevent directory traversal by realpath comparison
    try:
        images_dir_real = os.path.realpath(images_dir)
        requested_real = os.path.realpath(requested_path)
    except Exception:
        # Any failure in path resolution is treated as invalid
        abort(400, description='Invalid file path')

    if not requested_real.startswith(images_dir_real + os.sep):
        # The file is outside the images directory
        abort(400, description='Invalid file path')

    # 5) Check that the file actually exists and is a regular file
    if not os.path.isfile(requested_real):
        abort(404, description='File not found')

    # 6) Serve the file
    # as_attachment=False → inline display; adjust headers as needed
    response = send_from_directory(directory=images_dir,
                                   filename=file_name,
                                   as_attachment=False)

    # 7) (Optional) tighten cache headers
    response.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, max-age=0'
    return response