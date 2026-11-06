from flask import send_from_directory, abort, escape
from flask import Flask, request, redirect
import os

app = Flask(__name__)

@app.route('/img')
def img():
    # Validate and sanitise input
    filename = request.args.get('file', '', type=str)
    
    # Only allow files with safe image extensions
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    if not filename or '.' not in filename:
        abort(400, description="Invalid filename")
        
    name, ext = filename.rsplit('.', 1)
    ext = ext.lower()
    if ext not in allowed_exts:
        abort(400, description="Unsupported file type")
        
    # Prevent path traversal
    if '/' in filename or '\\' in filename or filename.startswith('.'):
        abort(400, description="Invalid filename")
    
    # Only allow safe characters in filename
    import re
    if not re.match(r'^[A-Za-z0-9_.-]+$', filename):
        abort(400, description="Invalid characters in filename")
    
    image_folder = os.path.join(os.path.dirname(__file__), 'images')
    
    try:
        # Serve file securely, prevent XSS/other issues
        response = send_from_directory(image_folder, filename)
        # Set security headers
        response.headers['X-Content-Type-Options'] = 'nosniff'
        response.headers['Content-Security-Policy'] = "default-src 'none'; img-src 'self'"
        response.headers['X-Frame-Options'] = 'DENY'
        response.headers['X-XSS-Protection'] = '1; mode=block'
        return response
    except Exception:
        abort(404, description="File not found")