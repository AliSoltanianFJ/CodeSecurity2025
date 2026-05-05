
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
CSP_POLICY = "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self';"

app.config['MAX_CONTENT_LENGTH'] = MAX_FILE_SIZE

def allowed_file(filename):
    """Check if the file extension is allowed."""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.after_request
def add_security_headers(response):
    """Add security headers to every response."""
    response.headers['Content-Security-Policy'] = CSP_POLICY
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['X-XSS-Protection'] = '1; mode=block'
    response.headers['Cache-Control'] = 'no-store' #Prevent caching
    response.headers['Pragma'] = 'no-cache'
    return response

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    """
    Securely retrieves and serves an image from the specified directory.

    This function addresses path traversal vulnerabilities by:
    1. Using a hardcoded, safe base directory for images.
    2. Sanitizing the filename using secure_filename to prevent directory traversal.
    3. Checking if the requested file exists within the allowed directory.
    4. Enforcing allowed file extensions.
    5. Implementing robust error handling and logging.
    """
    filename = request.args.get('filename')

    if not filename:
        logging.warning("Image request without filename.")
        abort(400, "Filename parameter is required.")

    # Sanitize the filename to prevent path traversal.
    filename = secure_filename(filename)

    if not allowed_file(filename):
        logging.warning(f"Attempted access to file with invalid extension: {filename}")
        abort(400, "Invalid file type.")

    # Construct the full path to the requested file.
    filepath = os.path.join(IMAGE_FOLDER, filename)

    # Check if the file exists within the allowed directory.  This is CRITICAL.
    if not os.path.isfile(filepath):
        logging.warning(f"File not found: {filepath}")
        abort(404, "File not found.")
    
    # resolves any symbolic links
    if not os.path.realpath(filepath).startswith(os.path.realpath(IMAGE_FOLDER)):
        logging.warning(f"Attempted path traversal: {filepath}")
        abort(400, "Invalid request.")

    try:
        # Serve the file using send_from_directory, which handles proper content encoding.
        return send_from_directory(IMAGE_FOLDER, filename)
    except Exception as e:
        logging.exception(f"Error serving file: {filename}")
        abort(500, "Internal server error.")

if __name__ == '__main__':
    # Never run in debug mode in production
    app.run(debug=False, host='0.0.0.0', port=5000)