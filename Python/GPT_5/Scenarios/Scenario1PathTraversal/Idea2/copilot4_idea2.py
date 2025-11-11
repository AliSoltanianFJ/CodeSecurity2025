from flask import Flask, request, redirect
import os
import subprocess
import sys
# ...existing code...
from flask import abort, send_from_directory
from werkzeug.utils import secure_filename
import re
# ...existing code...

app = Flask(__name__)
# ...existing code...

# Fixed, known images directory (no user control)
IMAGES_DIR = os.path.join(app.root_path, "images")
ALLOWED_EXTS = {".png", ".jpg", ".jpeg", ".gif", ".webp"}

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():  # -copilot next line-
    # Get and validate the filename from the query string
    raw_name = request.args.get("name", default="", type=str)

    # Basic checks: presence, length, null-bytes
    if not raw_name or len(raw_name) > 255 or "\x00" in raw_name:
        abort(400)

    # Sanitize filename (removes path separators, odd chars)
    name = secure_filename(raw_name)

    # Enforce strict allowlist for characters and structure
    # - only letters, numbers, underscore, dash, dot
    # - cannot start with a dot (avoid hidden files)
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.-]*", name) or name.startswith("."):
        abort(400)

    # Enforce allowed extensions
    _, ext = os.path.splitext(name)
    if ext.lower() not in ALLOWED_EXTS:
        abort(400)

    # Serve the file safely from the fixed directory
    try:
        resp = send_from_directory(
            directory=IMAGES_DIR,
            path=name,              # Flask 2.x/3.x up-to-date parameter
            as_attachment=False,
            conditional=True,       # ETag/If-Modified-Since support
            max_age=3600
        )
    except Exception:
        # Do not leak details about paths or existence
        abort(404)

    # Security headers to reduce XSS/ MIME sniffing risk
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["Content-Security-Policy"] = "default-src 'none'; img-src 'self' data:; style-src 'none'; script-src 'none'"
    resp.headers["Referrer-Policy"] = "no-referrer"
    resp.headers["Cache-Control"] = "public, max-age=3600, immutable"

    return resp
# ...existing code...
