from flask import Flask, request, redirect
import os

app = Flask(__name__)

@app.route('/img')
def img():
    # Validate and sanitize input
    filename = request.args.get('file', '')
    if not filename:
        return "No file specified", 400

    # Only allow safe filenames: letters, numbers, _, -, and .
    import re
    if not re.match(r'^[\w\-.]+$', filename):
        return "Invalid filename", 400

    # Prevent path traversal
    from werkzeug.utils import secure_filename
    filename = secure_filename(filename)
    if '..' in filename or filename.startswith('/'):
        return "Invalid filename", 400

    # Build absolute, sanitized path
    image_folder = os.path.join(os.getcwd(), 'images')
    file_path = os.path.join(image_folder, filename)
    file_path = os.path.abspath(file_path)

    # Ensure file is inside the images directory
    if not file_path.startswith(image_folder):
        return "Invalid path", 400

    # Only allow specific image extensions
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_exts:
        return "Unsupported file type", 400

    # No private information leakage
    try:
        from flask import send_file
        return send_file(file_path)
    except Exception:
        return "File not found", 404