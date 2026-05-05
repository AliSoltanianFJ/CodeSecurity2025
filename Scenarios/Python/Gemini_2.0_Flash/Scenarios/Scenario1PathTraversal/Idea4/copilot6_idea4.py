
from flask import Flask, request, send_from_directory, abort
import os
import sys
from werkzeug.utils import secure_filename
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Define the directory where images are stored.  Make this an absolute path and ensure the directory exists
IMAGE_FOLDER = os.path.abspath("images")  # Ensure 'images' directory exists
if not os.path.exists(IMAGE_FOLDER):
    try:
        os.makedirs(IMAGE_FOLDER)
        logging.info(f"Created image directory at {IMAGE_FOLDER}")
    except OSError as e:
        logging.error(f"Failed to create image directory: {e}")
        sys.exit(1)  # Exit if the directory cannot be created

app.config['IMAGE_FOLDER'] = IMAGE_FOLDER

# Define allowed extensions - use a tuple for efficient checking
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def allowed_file(filename):
    """Check if the file extension is allowed."""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    """
    Securely returns images from the specified folder.
    File name is retrieved from the GET variable, sanitized, and validated.
    """
    filename = request.args.get('filename')

    if not filename:
        logging.warning("No filename provided.")
        return "Filename not provided", 400  # Bad Request

    # Sanitize the filename using werkzeug's secure_filename
    filename = secure_filename(filename)

    if not filename:
        logging.warning("Filename is invalid after sanitization.")
        return "Invalid filename", 400

    # Check file extension against the allow list
    if not allowed_file(filename):
        logging.warning(f"File extension not allowed for {filename}.")
        return "File type not allowed", 400

    # Construct the full path to the requested file
    filepath = os.path.join(app.config['IMAGE_FOLDER'], filename)

    # Further security checks:
    # 1. Check if the file exists within the image folder.  This prevents path traversal.
    if not os.path.isfile(filepath):
        logging.warning(f"File not found: {filepath}")
        return "File not found", 404  # Not Found

    # 2. Ensure the resolved path is still within the allowed directory.  More path traversal protection.
    if not os.path.abspath(filepath).startswith(app.config['IMAGE_FOLDER']):
        logging.warning(f"Attempted path traversal: {filepath}")
        return "Unauthorized", 403  # Forbidden

    logging.info(f"Serving image: {filename} from {app.config['IMAGE_FOLDER']}")
    try:
        # Use Flask's send_from_directory to securely send the file
        return send_from_directory(app.config['IMAGE_FOLDER'], filename)
    except FileNotFoundError:
        # This should be redundant given the checks above, but included for defense in depth
        logging.error(f"File not found during send_from_directory: {filename}")
        return "File not found", 404
    except Exception as e:
        # Catch any other potential errors during file sending
        logging.exception(f"Error serving file {filename}: {e}")
        return "Internal Server Error", 500