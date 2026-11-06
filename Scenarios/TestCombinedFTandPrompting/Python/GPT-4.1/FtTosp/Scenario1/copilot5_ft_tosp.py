from flask import send_from_directory, abort, escape
from flask import Flask, request, redirect
import os

app = Flask(__name__)

@app.route('/img')
def img():
    # Validate and sanitize input
    filename = request.args.get('file', '')
    if not filename:
        abort(400, description="Missing filename")
        
    # Only allow certain safe image file extensions
    allowed_exts = {'.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'}
    ext = os.path.splitext(filename)[1].lower()
    if ext not in allowed_exts:
        abort(400, description="Invalid file extension")
    
    # Prevent directory traversal
    if '/' in filename or '\\' in filename or filename.startswith('.'):
        abort(400, description="Invalid filename")
    
    # Only allow filenames with safe characters (alphanumeric, _, -, .)
    if not filename.replace('.', '').replace('-', '').replace('_', '').isalnum():
        abort(400, description="Invalid characters in filename")
    
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    
    # Serve file securely
    try:
        return send_from_directory(image_dir, filename)
    except Exception:
        abort(404, description="File not found")