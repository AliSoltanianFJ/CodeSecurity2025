

from flask import Flask, request, send_from_directory, abort
import os
import sys
from werkzeug.utils import secure_filename
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Define the directory where images are stored.  Make sure this path is safe and not user-controlled.
IMAGE_FOLDER = os.path.abspath("images")  # Use absolute path
if not os.path.exists(IMAGE_FOLDER):
    try:
        os.makedirs(IMAGE_FOLDER)
    except OSError as e:
        logging.error(f"Failed to create image directory: {e}")
        sys.exit(1)

# Define allowed extensions - use a very strict allow list
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

# Secure filename function
def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('filename')

    if not filename:
        logging.warning("Filename not provided.")
        return "Filename missing", 400

    # Secure the filename before use
    filename = secure_filename(filename)

    if not allowed_file(filename):
        logging.warning(f"Invalid filename extension: {filename}")
        return "Invalid file type", 400

    # Construct the full path to the image
    filepath = os.path.join(IMAGE_FOLDER, filename)

    # Validate the file exists within the allowed directory
    if not os.path.isfile(filepath):
        logging.warning(f"File not found: {filepath}")
        return "File not found", 404

    # Further security check: Ensure the path is still within the base directory after joining.
    # This prevents directory traversal attacks even if secure_filename fails.
    filepath = os.path.abspath(filepath)
    if not filepath.startswith(os.path.abspath(IMAGE_FOLDER)):
        logging.warning(f"Attempted path traversal: {filepath}")
        return "Unauthorized", 403

    logging.info(f"Serving image: {filename} from {filepath}")
    try:
        return send_from_directory(IMAGE_FOLDER, filename)
    except Exception as e:
        logging.error(f"Error serving file: {e}")
        return "Internal Server Error", 500