
from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)
app.config.update(
    DEBUG=False,            # enforce non-debug mode in production
    SEND_FILE_MAX_AGE_DEFAULT=300  # cache images for 5 minutes
)

# Define the absolute images directory (no directory listings, file perms should be read-only)
IMAGES_DIR = os.path.join(app.root_path, 'images')

@app.route('/img')
def img():
    """
    Securely serve an image from the 'images' folder.
    GET parameter: file=<filename>
    """
    # 1. Input validation � must provide 'file' parameter
    filename = request.args.get('file')
    if not filename:
        abort(400, description='Missing "file" parameter.')

    # 2. Sanitize filename (strip path chars, only allow ascii)
    filename = secure_filename(filename)
    if not filename:
        abort(400, description='Invalid filename after sanitization.')

    # 3. Enforce extension whitelist
    _, ext = os.path.splitext(filename)
    if ext.lower() not in {'.png', '.jpg', '.jpeg', '.gif'}:
        abort(400, description='Unsupported file type.')

    # 4. Build absolute path and enforce directory constraint
    requested_path = os.path.realpath(os.path.join(IMAGES_DIR, filename))
    images_dir_real = os.path.realpath(IMAGES_DIR)
    # Path traversal prevention attacker cannot escape IMAGES_DIR
    if not requested_path.startswith(images_dir_real + os.sep):
        abort(403, description='Access to the requested resource is forbidden.')

    # 5. Verify file exists and is a regular file
    if not os.path.isfile(requested_path):
        abort(404, description='File not found.')

    # 6. Send file (Flask handles safe headers & MIME type)
    return send_from_directory(
        directory=IMAGES_DIR,
        filename=filename,
        as_attachment=False,
        conditional=True
    )

if __name__ == '__main__':
    # In production, use a WSGI server (Gunicorn/Waitress) behind TLS termination
    app.run(host='0.0.0.0', port=5000)