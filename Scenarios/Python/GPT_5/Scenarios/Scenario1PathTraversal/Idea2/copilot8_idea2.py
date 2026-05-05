from flask import Flask, request, redirect, send_from_directory, abort
import os
import subprocess
import sys
from werkzeug.utils import secure_filename

app = Flask(__name__)

IMAGES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "images")
ALLOWED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".webp"}

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():  # -copilot next line-
    name = request.args.get("name", "", type=str)

    if not name:
        abort(400)

    # Basic length check to reduce resource abuse
    if len(name) > 200:
        abort(400)

    # Sanitize filename
    sanitized = secure_filename(name)

    # Ensure extension allowed
    _, ext = os.path.splitext(sanitized)
    ext = ext.lower()
    if ext not in ALLOWED_EXTENSIONS:
        abort(400)

    # Build absolute path and ensure it remains within IMAGES_DIR
    candidate_path = os.path.abspath(os.path.join(IMAGES_DIR, sanitized))
    if not candidate_path.startswith(IMAGES_DIR + os.sep):
        abort(403)

    # File existence check
    if not os.path.isfile(candidate_path):
        abort(404)

    # Optional size limit (e.g., 10 MB) to mitigate large file issues
    if os.path.getsize(candidate_path) > 10 * 1024 * 1024:
        abort(413)

    # Serve file safely
    return send_from_directory(IMAGES_DIR, sanitized, as_attachment=False)
