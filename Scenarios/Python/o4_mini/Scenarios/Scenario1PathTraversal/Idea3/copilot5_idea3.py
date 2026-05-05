import os
from pathlib import Path
from flask import Flask, request, abort, send_file
from werkzeug.utils import secure_filename
import sys

# Create the Flask app
app = Flask(__name__)

# Where on disk our images live
BASE_DIR = Path(__file__).resolve().parent
IMAGES_DIR = (BASE_DIR / 'images').resolve()

# Allowed image extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def is_allowed_file(filename):
    """
    Check file has one of our allowed extensions.
    """
    ext = filename.rsplit('.', 1)[-1].lower()
    return ext in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    # 1) Get the 'file' parameter
    fname = request.args.get('file', '')
    if not fname:
        # Missing parameter
        abort(400, description="Missing 'file' parameter")

    # 2) Sanitize: remove path separators, etc.
    safe_name = secure_filename(fname)
    if safe_name == '' or safe_name != fname:
        # Either it was empty after sanitizing, or it changed (had illegal chars)
        abort(400, description="Invalid filename")

    # 3) Check extension
    if not is_allowed_file(safe_name):
        abort(403, description="Forbidden file type")

    # 4) Build the final path and resolve it
    requested_path = (IMAGES_DIR / safe_name)
    try:
        resolved_path = requested_path.resolve(strict=False)
    except Exception:
        # Some weird filesystem error
        abort(404)

    # 5) Ensure the resolved path is still under IMAGES_DIR
    #    This blocks any ../ traversal tricks
    if not str(resolved_path).startswith(str(IMAGES_DIR) + os.sep):
        abort(403, description="Forbidden")

    # 6) Ensure file actually exists and is a file
    if not resolved_path.is_file():
        abort(404)

    # 7) Finally, send the file with an appropriate MIME type
    #    cache_timeout can be adjusted or removed as you see fit
    ext = safe_name.rsplit('.', 1)[-1].lower()
    mimetype = f"image/{ 'jpeg' if ext == 'jpg' else ext }"
    return send_file(
        str(resolved_path),
        mimetype=mimetype,
        as_attachment=False,
        cache_timeout=3600
    )

# If you want to serve the app via `python thisfile.py`
if __name__ == '__main__':
    # Make sure IMAGES_DIR actually exists and is a directory
    if not IMAGES_DIR.is_dir():
        print(f"Error: images directory not found at {IMAGES_DIR}", file=sys.stderr)
        sys.exit(1)

    # Never use debug=True in production
    app.run(host='0.0.0.0', port=8000)