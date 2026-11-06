from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # local imports so we don’t need to touch the top of your file
    from flask import abort, send_file
    import re, mimetypes

    # 1. Fetch & basic check
    filename = request.args.get('file', type=str)
    if not filename:
        # 400 Bad Request if no 'file' parameter
        abort(400, description='No file specified')

    # 2. Whitelist‐style validation: only letters, digits, underscore, hyphen,
    #    followed by a single ‘.’ and one of the allowed extensions.
    VALID_NAME = re.compile(r'^[A-Za-z0-9_-]+\.(?:jpe?g|png|gif)$', re.IGNORECASE)
    if not VALID_NAME.fullmatch(filename):
        abort(400, description='Invalid file name')

    # 3. Construct the absolute path to the images directory
    base_dir   = os.path.dirname(os.path.abspath(__file__))
    images_dir = os.path.join(base_dir, 'images')

    # 4. Join and resolve the requested file’s real path
    requested_path = os.path.join(images_dir, filename)
    real_path      = os.path.realpath(requested_path)

    # 5. Ensure the resolved path is still inside images_dir
    #    (protects against ../ and symlink attacks)
    if not real_path.startswith(images_dir + os.sep):
        abort(400, description='Invalid file path')

    # 6. Make sure it’s a regular file that exists
    if not os.path.isfile(real_path):
        abort(404, description='File not found')

    # 7. Guess the MIME type (falls back to octet‐stream)
    mime_type, _ = mimetypes.guess_type(real_path)
    if not mime_type:
        mime_type = 'application/octet-stream'

    # 8. Serve the file
    #    as_attachment=False so browsers will display it inline
    return send_file(real_path, mimetype=mime_type, as_attachment=False)
