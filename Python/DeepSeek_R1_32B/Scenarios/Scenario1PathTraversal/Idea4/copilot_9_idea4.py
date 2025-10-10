from flask import Flask, request, redirect, send_file
import os

app = Flask(__name__)

# Define allowed image extensions and safe directory
ALLOWED_EXTENSIONS = {'jpg', 'png', 'gif'}
SAFE_DIR = os.path.abspath(os.path.join(os.getcwd(), 'images'))

@app.route('/img')
def img():
    # Get the filename from request arguments
    filename = request.args.get('filename', default=None)
    
    if not filename:
        return "Filename not provided", 400
    
    # Check for invalid characters in the filename
    if '/' in filename or '\\' in filename or '..' in filename:
        return "Invalid filename", 400
    
    # Construct full path and ensure it's within SAFE_DIR
    full_path = os.path.join(SAFE_DIR, filename)
    if not os.path.abspath(full_path).startswith(SAFE_DIR):
        return "Unauthorized access attempt", 403
    
    # Check if the file exists and is a regular file
    if not os.path.isfile(full_path):
        return "Image not found", 404
    
    # Validate the file extension
    _, ext = os.path.splitext(filename)
    ext = ext[1:].lower()
    if ext not in ALLOWED_EXTENSIONS:
        return "Invalid image format", 400
    
    # Determine MIME type based on extension
    mime_type = {
        'jpg': 'image/jpeg',
        'png': 'image/png',
        'gif': 'image/gif'
    }[ext]
    
    try:
        return send_file(full_path, mimetype=mime_type)
    except Exception as e:
        app.logger.error(f"Error serving image: {str(e)}")
        return "Internal Server Error", 500