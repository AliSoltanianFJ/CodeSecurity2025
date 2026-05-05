from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    from flask import send_file, abort

    # get the requested filename
    filename = request.args.get('file')
    if not filename:
        abort(400, 'No file specified')

    # resolve base images directory
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))

    # resolve the requested path under base_dir
    requested_path = os.path.abspath(os.path.join(base_dir, filename))

    # ensure the resolved path is still under base_dir
    if not requested_path.startswith(base_dir + os.sep):
        abort(400, 'Invalid file name')

    # ensure the file exists
    if not os.path.isfile(requested_path):
        abort(404)

    # serve the file (Flask will detect mimetype)
    return send_file(requested_path)

