from flask import Flask, request, redirect
import os
import subprocess
import sys
import os
import re
import imghdr
from flask import request, abort, send_file

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # 1) Get and normalize the user-supplied filename
    filename = request.args.get('file', '').strip()
    if not filename:
        abort(400, description="Missing 'file' parameter")

    # 2) Validate against a strict pattern: name.ext where ext∈{png,jpg,jpeg,gif}
    #    - Alphanumeric, underscores, hyphens only in the name
    #    - Exactly one dot before the extension
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif'}
    if not re.fullmatch(r'[A-Za-z0-9_-]+\.(?:png|jpe?g|gif)', filename, flags=re.IGNORECASE):
        abort(400, description="Invalid filename format")

    # 3) Resolve absolute paths
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))
    requested_path = os.path.abspath(os.path.join(base_dir, filename))

    # 4) Enforcement of directory confinement
    if not requested_path.startswith(base_dir + os.sep):
        abort(400, description="Invalid file path")

    # 5) Existence check, regular file check, symlink check
    if not os.path.exists(requested_path) or not os.path.isfile(requested_path):
        abort(404, description="File not found")
    if os.path.islink(requested_path):
        abort(404, description="File not found")

    # 6) Verify actual file content matches a known image type
    kind = imghdr.what(requested_path)
    if kind == 'jpeg':
        mimetype = 'image/jpeg'
    elif kind == 'png':
        mimetype = 'image/png'
    elif kind == 'gif':
        mimetype = 'image/gif'
    else:
        # could be zero-length or bogus data
        abort(404, description="Not a supported image format")

    # 7) Serve the file with correct MIME type
    response = send_file(requested_path, mimetype=mimetype, conditional=True)
    return response
