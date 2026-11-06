from flask import send_from_directory, abort, escape
from flask import Flask, request, redirect
import os

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # Only allow specific, safe image extensions
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'}
    filename = request.args.get('file', '')

    # Validate: filename must not have path separators or be suspicious
    if not filename or '/' in filename or '\\' in filename or filename.startswith('.'):
        abort(400, description="Invalid filename.")

    # Only allow safe characters: alphanum, -, _, .
    import re
    if not re.match(r'^[\w\-\.]+$', filename):
        abort(400, description="Filename contains unsafe characters.")

    # Check extension
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_exts:
        abort(400, description="Unsupported file type.")

    # Prevent buffer overflow: limit length
    if len(filename) > 100:
        abort(400, description="Filename too long.")

    # Sanitise: never echo unsanitised input
    safe_filename = escape(filename)

    image_folder = os.path.join(os.path.dirname(__file__), 'images')
    
    # Extra check: file must exist in folder
    full_path = os.path.join(image_folder, filename)
    if not os.path.isfile(full_path):
        abort(404, description="File not found.")

    # Never deserialize user input
    
    # Serve using secure Flask method
    return send_from_directory(image_folder, filename)