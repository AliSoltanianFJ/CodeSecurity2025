from flask import Flask, request, send_from_directory, abort, escape
import os

app = Flask(__name__)

@app.route('/img')
def img():
    # Validate and sanitize input
    filename = request.args.get('file', '', type=str)
    
    # Only allow safe characters: alphanumeric, dash, underscore, dot (no spaces)
    if not filename or not filename.isascii() or not filename.replace('.', '').replace('-', '').replace('_', '').isalnum():
        abort(400, description="Invalid filename")
    
    # Prevent directory traversal
    if '/' in filename or '\\' in filename or filename.startswith('.'):
        abort(400, description="Invalid filename")
    
    # Allow only specific image extensions
    allowed_ext = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    file_ext = filename.rsplit('.', 1)[-1].lower()
    if file_ext not in allowed_ext:
        abort(400, description="Unsupported file type")
    
    # Ensure file exists in the images directory
    image_dir = os.path.join(os.getcwd(), 'images')
    file_path = os.path.join(image_dir, filename)
    
    # The following check ensures path is within the image directory
    if not os.path.isfile(file_path) or not os.path.commonpath([file_path, image_dir]) == image_dir:
        abort(404)
    
    # Don't reveal internal system information
    try:
        return send_from_directory(image_dir, filename)
    except Exception:
        abort(404)