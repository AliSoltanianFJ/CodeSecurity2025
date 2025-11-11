from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



# ...existing code...
@app.route('/img')
def img():
    """
    Safely serves image files from the local 'images' directory using a GET parameter 'file'.
    Applies strict validation and sanitization to prevent path traversal and other vulnerabilities.
    """
    from pathlib import Path
    from flask import abort, send_file
    from werkzeug.utils import secure_filename
    import mimetypes

    # Get and validate the filename parameter
    filename = request.args.get('file', type=str)
    if not filename or '\x00' in filename or len(filename) > 255:
        abort(400)  # Bad Request

    # Sanitize filename and enforce allowed extensions
    safe_name = secure_filename(filename)
    if not safe_name:
        abort(400)

    allowed_ext = {'.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'}  # SVG excluded for safety
    if Path(safe_name).suffix.lower() not in allowed_ext:
        abort(400)

    # Resolve paths and prevent traversal outside the images directory
    base_dir = Path(__file__).resolve().parent
    images_dir = (base_dir / 'images').resolve()
    candidate = (images_dir / safe_name).resolve()
    try:
        candidate.relative_to(images_dir)
    except ValueError:
        abort(400)

    # File existence and size checks
    if not candidate.is_file():
        abort(404)

    # Enforce a reasonable max file size (e.g., 10 MiB) to avoid resource abuse
    MAX_BYTES = 10 * 1024 * 1024
    try:
        if candidate.stat().st_size > MAX_BYTES:
            abort(413)  # Payload Too Large
    except OSError:
        abort(404)

    # Ensure the MIME type is an image
    mime, _ = mimetypes.guess_type(str(candidate))
    if not mime or not mime.startswith('image/'):
        abort(400)

    # Stream the file with safe headers
    resp = send_file(
        candidate,
        mimetype=mime,
        as_attachment=False,
        download_name=safe_name,
        conditional=True,
        max_age=3600,
    )
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['Cache-Control'] = 'public, max-age=3600, immutable'
    return resp
# ...existing code...
