from flask import Flask, request, abort, send_from_directory, make_response
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# configuration
BASE_DIR = os.path.abspath(os.path.dirname(__file__))
IMAGES_DIR = os.path.join(BASE_DIR, 'images')
# only allow these extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}

@app.route('/img')
def img():
    # 1. Get the raw filename parameter.
    filename = request.args.get('file', '').strip()
    if not filename:
        # missing parameter
        abort(400, description='Missing "file" parameter.')

    # 2. Sanitize: remove path separators, control chars, etc.
    filename = secure_filename(filename)
    if not filename:
        # after sanitization, name is empty
        abort(400, description='Invalid filename.')

    # 3. Enforce allowed extension
    name_part, dot, ext = filename.rpartition('.')
    if dot == '' or ext.lower() not in ALLOWED_EXTENSIONS:
        abort(400, description='Disallowed file type.')

    # 4. Resolve full filesystem path and ensure it's under IMAGES_DIR
    requested_path = os.path.abspath(os.path.join(IMAGES_DIR, filename))
    # os.path.commonpath is safer than startswith
    if os.path.commonpath([IMAGES_DIR, requested_path]) != IMAGES_DIR:
        abort(400, description='Invalid file path.')

    # 5. Check existence and basic size limit to avoid DoS
    try:
        stat = os.stat(requested_path)
    except OSError:
        # file not found or inaccessible
        abort(404)
    # optional: reject extremely large files (e.g. > 10 MB)
    MAX_SIZE = 10 * 1024 * 1024
    if stat.st_size > MAX_SIZE:
        abort(403, description='File too large.')

    # 6. Stream the file back with correct MIME and caching headers
    response = make_response(
        send_from_directory(
            IMAGES_DIR,
            filename,
            as_attachment=False,
            conditional=True  # enable HTTP 304 handling
        )
    )
    # Recommended caching headers (adjust max-age as you see fit)
    response.headers['Cache-Control'] = 'public, max-age=3600'
    return response