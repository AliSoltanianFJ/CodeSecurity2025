from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    from flask import abort, Response, make_response
    import pathlib
    import mimetypes

    filename = request.args.get('file', '')
    if not filename:
        return make_response("No file specified", 400)

    # Only allow specific image extensions
    allowed_exts = {'.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'}
    file_ext = pathlib.Path(filename).suffix.lower()
    if file_ext not in allowed_exts:
        return make_response("Unsupported file type", 400)

    # Must match strict filename pattern: letters, numbers, _, -, .
    import re
    if not re.fullmatch(r'[A-Za-z0-9_\-\.]+', filename):
        return make_response("Invalid filename", 400)

    # Resolve absolute paths securely
    images_dir = pathlib.Path(__file__).parent / 'images'
    target_path = images_dir / filename
    try:
        resolved_path = target_path.resolve(strict=True)
    except FileNotFoundError:
        return make_response("File not found", 404)

    # Ensure the file is inside the images directory
    if images_dir not in resolved_path.parents:
        abort(403)

    # Prevent serving hidden files
    if filename.startswith('.'):
        abort(403)

    # Detect and set correct MIME type, default to octet-stream
    mime_type, _ = mimetypes.guess_type(resolved_path.as_posix())
    if mime_type is None or not mime_type.startswith('image/'):
        mime_type = 'application/octet-stream'

    # Serve file content securely
    try:
        with open(resolved_path, 'rb') as img_file:
            data = img_file.read()
    except Exception:
        abort(500)

    resp = Response(data, mimetype=mime_type)
    # Set Content-Disposition to prevent XSS via filename
    resp.headers['Content-Disposition'] = 'inline; filename="{}"'.format(filename.replace('"', ''))
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    return resp