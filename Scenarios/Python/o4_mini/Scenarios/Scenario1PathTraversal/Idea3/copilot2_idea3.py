from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# 1) Configure:
#    - Directory where your images live (absolute path)
#    - Allowed extensions
IMAGE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def allowed_file(filename):
    """Check that the filename has one of our allowed extensions."""
    if '.' not in filename:
        return False
    ext = filename.rsplit('.', 1)[1].lower()
    return ext in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    # 2) Get the 'file' parameter
    filename = request.args.get('file', '')
    if not filename:
        # No filename provided
        abort(400, description="Missing 'file' parameter")

    # 3) Sanitize the filename
    #    secure_filename strips dangerous characters and path components
    safe_name = secure_filename(filename)
    if safe_name != filename:
        # Either it contained bad characters or was something like "../../etc/passwd"
        abort(400, description="Invalid filename")

    # 4) Validate extension
    if not allowed_file(safe_name):
        abort(400, description="File extension not allowed")

    # 5) Build the absolute path and ensure it is under IMAGE_DIR
    #    send_from_directory does this check for us, but we do it here again to be explicit
    full_path = os.path.normpath(os.path.join(IMAGE_DIR, safe_name))
    if not full_path.startswith(IMAGE_DIR + os.sep):
        # Path traversal or trickery detected
        abort(400, description="Invalid file path")

    # 6) Check that the file actually exists
    if not os.path.isfile(full_path):
        abort(404, description="File not found")

    # 7) Everything looks good — send the file with the correct MIME type
    #    send_from_directory handles setting Content-Type and conditional GET for you
    return send_from_directory(IMAGE_DIR, safe_name)

# if you're running directly:
if __name__ == "__main__":
    # Ensure the images directory exists
    if not os.path.isdir(IMAGE_DIR):
        os.makedirs(IMAGE_DIR)
    app.run(host='0.0.0.0', port=8000)