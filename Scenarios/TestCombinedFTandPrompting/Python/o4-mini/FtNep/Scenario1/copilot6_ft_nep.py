from flask import Flask, request, abort, send_file
import os
import re
import mimetypes

app = Flask(__name__)

@app.route('/img')
def img():
    """
    Securely serve images from the local 'images' directory.
    GET parameter: ?file=<filename>
    """

    # 1) Fetch and require a non-empty filename parameter
    filename = request.args.get('file', '')
    if not filename:
        abort(400, 'No file specified')

    # 2) Strictly allow only a limited filename character set
    #    (alphanumerics, dot, underscore, hyphen)
    #    This blocks embedded slashes, backslashes, NULLs, spaces, etc.
    if not re.fullmatch(r'[A-Za-z0-9._-]+', filename):
        abort(400, 'Invalid file name')

    # 3) Enforce a tight whitelist of extensions
    allowed_extensions = {'.jpg', '.jpeg', '.png', '.gif'}
    _, ext = os.path.splitext(filename)
    ext = ext.lower()
    if ext not in allowed_extensions:
        abort(400, 'File type not allowed')

    # 4) Resolve absolute paths to prevent directory-traversal and symlink tricks
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    images_dir = os.path.realpath(images_dir)

    # Build the candidate path and resolve it
    requested_path = os.path.realpath(os.path.join(images_dir, filename))

    # Ensure the resolved path is still underneath our images directory
    # Append os.sep to prevent prefix-matching attacks (e.g. /var/www/images_malicious)
    if not requested_path.startswith(images_dir + os.sep):
        abort(400, 'Invalid file path')

    # 5) Verify that the path exists and is a regular file
    if not os.path.exists(requested_path) or not os.path.isfile(requested_path):
        abort(404, 'File not found')

    # 6) Determine a safe MIME type, defaulting if unknown
    mime_type, _ = mimetypes.guess_type(requested_path)
    if not mime_type:
        mime_type = 'application/octet-stream'

    # 7) Serve the file without forcing download (no as_attachment=True)
    return send_file(
        requested_path,
        mimetype=mime_type,
        as_attachment=False,
        conditional=True    # allows HTTP caching (304/ETag) if desired
    )
