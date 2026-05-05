
from flask import Flask, request, send_from_directory, abort
import os
import sys
from werkzeug.utils import secure_filename
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Define the directory where images are stored.  Make sure this path is safe and not user-controlled.
IMAGE_FOLDER = os.path.join(app.root_path, 'images')
# Ensure the image folder exists
os.makedirs(IMAGE_FOLDER, exist_ok=True)

# List of allowed extensions (more restrictive is better).
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

# Maximum file size (in bytes) to prevent DoS.  Adjust as needed.
MAX_FILE_SIZE = 1024 * 1024  # 1MB

# CSP Header - mitigates XSS and other attacks
@app.after_request
def add_security_headers(response):
    response.headers['Content-Security-Policy'] = "default-src 'self'; script-src 'self'; object-src 'none'; img-src 'self' data:; media-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self';"
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['X-XSS-Protection'] = '1; mode=block'
    return response

def allowed_file(filename):
    """Check if the file extension is allowed."""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    """
    Securely returns images from the specified folder.
    """
    filename = request.args.get('filename')

    if not filename:
        logging.warning("Image request without filename.")
        return "Filename parameter is missing", 400

    # Sanitize the filename using werkzeug's secure_filename
    filename = secure_filename(filename)

    if not filename:
        logging.warning("Filename is invalid after sanitization.")
        return "Invalid filename", 400

    if not allowed_file(filename):
        logging.warning(f"File extension not allowed: {filename}")
        return "File type not allowed", 400

    # Construct the full path to the image
    filepath = os.path.join(IMAGE_FOLDER, filename)

    # Check if the file exists within the allowed directory.  This is CRITICAL.
    if not os.path.isfile(filepath):
        logging.warning(f"File not found: {filepath}")
        return "File not found", 404

    # Further check to ensure the file is within the allowed directory (defense in depth).
    if not os.path.abspath(filepath).startswith(os.path.abspath(IMAGE_FOLDER)):
        logging.warning(f"Attempted path traversal: {filepath}")
        return "Unauthorized access", 403

    try:
        # Check file size before sending
        file_size = os.path.getsize(filepath)
        if file_size > MAX_FILE_SIZE:
            logging.warning(f"File size exceeds maximum allowed size: {file_size}")
            return "File size exceeds maximum allowed size", 413

        # Use send_from_directory to securely send the file.
        return send_from_directory(IMAGE_FOLDER, filename)

    except Exception as e:
        logging.error(f"Error serving image: {e}")
        return "Internal Server Error", 500

if __name__ == '__main__':
    # Never run in debug mode in production
    app.run(debug=False, host='0.0.0.0', port=5000)