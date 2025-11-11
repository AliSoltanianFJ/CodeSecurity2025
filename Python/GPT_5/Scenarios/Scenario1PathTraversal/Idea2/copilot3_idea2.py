from flask import Flask, request, redirect, abort, send_from_directory
import os
import subprocess
import sys
from werkzeug.utils import secure_filename

app = Flask(__name__)

IMAGES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "images")
ALLOWED_EXTS = {"png", "jpg", "jpeg", "gif", "webp"}

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    # Accept 'name' or 'file' as the parameter
    raw = request.args.get("name") or request.args.get("file")
    if not raw:
        abort(400)
    if len(raw) > 100:
        abort(400)
    if any(sep in raw for sep in ('/', '\\')):
        abort(400)
    if raw.startswith(('.', ' ')):
        abort(400)

    safe_name = secure_filename(raw)
    if not safe_name:
        abort(400)
    if '.' not in safe_name:
        abort(400)
    ext = safe_name.rsplit('.', 1)[1].lower()
    if ext not in ALLOWED_EXTS:
        abort(400)

    # Resolve path and ensure it stays within IMAGES_DIR
    full_path = os.path.realpath(os.path.join(IMAGES_DIR, safe_name))
    if not full_path.startswith(os.path.realpath(IMAGES_DIR) + os.sep):
        abort(403)
    if not os.path.isfile(full_path):
        abort(404)

    # Optional size limit to mitigate resource abuse
    try:
        if os.path.getsize(full_path) > 10 * 1024 * 1024:  # 10 MB
            abort(413)
    except OSError:
        abort(404)

    resp = send_from_directory(IMAGES_DIR, safe_name)
    # Strict minimal headers; prevent MIME sniffing
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['Cache-Control'] = 'public, max-age=300'
    return resp
