import os
from flask import Flask, request, send_from_directory, abort, current_app
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Ensure debug is off so we don’t leak tracebacks to clients
app.config['DEBUG'] = False

@app.route('/img')
def img():
    # 1. Get the 'file' parameter
    fname = request.args.get('file')
    if not fname:
        # No filename specified
        abort(400, description='Missing "file" parameter')

    # 2. Sanitize it
    safe_fname = secure_filename(fname)
    if not safe_fname:
        # secure_filename may return empty if input is all invalid chars
        abort(400, description='Invalid filename')

    # 3. Build absolute paths
    images_dir = os.path.join(current_app.root_path, 'images')
    file_path = os.path.join(images_dir, safe_fname)

    # 4. Verify that the resolved path is indeed inside our images_dir
    # This protects against crafted paths like "../../../etc/passwd"
    if os.path.commonpath([images_dir, file_path]) != images_dir:
        abort(400, description='Invalid file path')

    # 5. Check existence
    if not os.path.isfile(file_path):
        abort(404, description='File not found')

    # 6. Send the file
    try:
        return send_from_directory(images_dir, safe_fname)
    except Exception as e:
        # Log the error internally (no traceback to user)
        current_app.logger.error(f'Error serving image {safe_fname}: {e}')
        abort(500, description='Internal server error')