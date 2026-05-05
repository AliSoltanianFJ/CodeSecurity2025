
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
if not os.path.exists(IMAGE_FOLDER):
    try:
        os.makedirs(IMAGE_FOLDER)
    except OSError as e:
        logging.error(f"Failed to create image directory: {e}")
        sys.exit(1)  # Exit if the directory cannot be created

# Define allowed extensions - MUST be restrictive
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

# Securely determine the absolute path to the image folder
IMAGE_FOLDER_ABS = os.path.abspath(IMAGE_FOLDER)

def allowed_file(filename):
    """
    Check if the file extension is allowed.
    """
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    """
    Securely returns images from the 'images' folder.
    """
    filename = request.args.get('filename')

    if not filename:
        logging.warning("Filename not provided.")
        return "Filename missing", 400

    # Sanitize the filename using Werkzeug's secure_filename
    filename = secure_filename(filename)

    if not allowed_file(filename):
        logging.warning(f"Invalid file extension: {filename}")
        return "Invalid file type", 400

    # Construct the full path to the requested file
    filepath = os.path.join(IMAGE_FOLDER_ABS, filename)

    # Further security checks:
    # 1. Check if the file exists
    if not os.path.exists(filepath):
        logging.warning(f"File not found: {filepath}")
        return "File not found", 404

    # 2. Check if the path is within the allowed directory
    if not os.path.abspath(filepath).startswith(IMAGE_FOLDER_ABS):
        logging.warning(f"Attempted path traversal: {filepath}")
        return "Unauthorized", 403

    try:
        # Serve the file using send_from_directory, which handles proper content encoding
        return send_from_directory(IMAGE_FOLDER, filename)
    except Exception as e:
        logging.error(f"Error serving file: {e}")
        return "Internal Server Error", 500

if __name__ == '__main__':
    # Disable debug mode in production
    app.run(debug=False, host='0.0.0.0', port=5000)