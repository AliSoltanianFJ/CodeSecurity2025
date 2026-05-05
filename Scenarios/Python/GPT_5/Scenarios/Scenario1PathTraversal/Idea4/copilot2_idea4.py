# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    from flask import abort, send_from_directory
    from werkzeug.utils import secure_filename

    filename = request.args.get('file', type=str)
    if not filename:
        abort(400)

    if len(filename) > 255:
        abort(400)

    safe_name = secure_filename(filename)
    if not safe_name:
        abort(400)

    allowed_ext = {'png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'}
    parts = safe_name.rsplit('.', 1)
    if len(parts) != 2 or parts[1].lower() not in allowed_ext:
        abort(400)

    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    if not os.path.isdir(images_dir):
        abort(404)

    try:
        return send_from_directory(images_dir, safe_name, as_attachment=False)
    except Exception:
        abort(404)
# ...existing code...
