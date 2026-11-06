from flask import Flask, request, redirect
import os

app = Flask(__name__)

@app.route('/img')
def img():
    from flask import send_from_directory, abort, escape

    # Only allow specific image file extensions
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    
    # Get and sanitize input
    filename = request.args.get('file', '', type=str)
    
    # Validate: filename present, length, no path traversal, valid extension
    if not filename:
        abort(400, description="Missing file parameter.")
    if len(filename) > 255:
        abort(400, description="Filename too long.")
    if '/' in filename or '\\' in filename or filename.startswith('.'):
        abort(400, description="Invalid filename.")
    if not '.' in filename:
        abort(400, description="No file extension specified.")
    
    file_ext = filename.rsplit('.', 1)[-1].lower()
    if file_ext not in ALLOWED_EXTENSIONS:
        abort(400, description="Unsupported file type.")
    
    # Prevent XSS: never reflect filename and never inject into HTML
    # No unsafe deserialization
    
    image_folder = os.path.join(os.path.dirname(__file__), 'images')
    
    try:
        # Only serve files from the images directory, never higher
        return send_from_directory(image_folder, filename)
    except Exception:
        abort(404, description="Image not found.")