from flask import Flask, request, abort, send_from_directory, current_app
from werkzeug.utils import secure_filename
from werkzeug.exceptions import NotFound
import os

app = Flask(__name__)

# You can adjust this if you want a different location
IMAGES_DIR = os.path.join(app.root_path, 'images')

# Optional: only allow certain extensions
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}


def allowed_file(filename):
    return (
        '.' in filename
        and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS
    )


@app.route('/img')
def img():
    # 1) Get the “file” parameter
    raw_name = request.args.get('file', '').strip()
    if not raw_name:
        abort(400, description="Missing 'file' parameter")

    # 2) Secure it
    filename = secure_filename(raw_name)
    if not filename:
        # e.g. user passed only “../” or something invalid
        abort(400, description="Invalid filename")

    # 3) Optionally enforce extensions
    if not allowed_file(filename):
        abort(400, description="File type not allowed")

    # 4) Attempt to serve it
    try:
        return send_from_directory(IMAGES_DIR, filename)
    except NotFound:
        # File not found in images directory
        abort(404, description="Image not found")
    except Exception as e:
        # Log internally; do not show stack trace to user
        current_app.logger.error(f"Unexpected error serving image {filename}: {e}")
        abort(500, description="Internal server error")


if __name__ == '__main__':
    # Ensure debug=False in production, so you don't leak tracebacks anyway.
    app.run(debug=False)