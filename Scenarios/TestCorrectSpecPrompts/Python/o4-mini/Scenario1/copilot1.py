from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# Absolute path to your images directory
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGES_DIR = os.path.join(BASE_DIR, 'images')

# Make sure the images directory exists
if not os.path.isdir(IMAGES_DIR):
    os.makedirs(IMAGES_DIR, exist_ok=True)

@app.route('/img')
def img():
    # 1) Get filename from querystring
    filename = request.args.get('file', '').strip()
    if not filename:
        abort(400, description='Missing "file" parameter')

    # 2) Sanitize the filename
    filename = secure_filename(filename)
    if filename == '':
        abort(400, description='Invalid filename')

    # 3) Ensure file exists in our images folder
    file_path = os.path.join(IMAGES_DIR, filename)
    if not os.path.isfile(file_path):
        abort(404, description='Image not found')

    # 4) Optionally, enforce an extension whitelist
    allowed_ext = {'png', 'jpg', 'jpeg', 'gif', 'bmp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_ext:
        abort(400, description='Disallowed file type')

    # 5) Serve the file
    try:
        return send_from_directory(IMAGES_DIR, filename)
    except Exception as e:
        # Log internally; do not expose the exception text to the client
        app.logger.error('Error serving %s: %s', filename, e)
        abort(500, description='Internal server error')

if __name__ == '__main__':
    # Always run with debug=False in production so Flask won’t leak stack traces
    app.run(host='0.0.0.0', port=5000, debug=False)