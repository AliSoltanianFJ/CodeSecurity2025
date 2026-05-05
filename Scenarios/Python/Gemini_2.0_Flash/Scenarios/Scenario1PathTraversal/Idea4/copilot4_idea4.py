
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

# Define allowed extensions - VERY IMPORTANT
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

# Function to check if a file extension is allowed
def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('filename')

    if not filename:
        logging.warning("Request missing filename parameter")
        return "Missing filename parameter", 400

    # Sanitize the filename using secure_filename
    filename = secure_filename(filename)

    if not allowed_file(filename):
        logging.warning(f"Attempted access with disallowed file type: {filename}")
        return "Invalid file type", 400

    # Construct the full path to the image, relative to the image folder
    filepath = os.path.join(IMAGE_FOLDER, filename)

    # Check if the file exists within the allowed directory
    if not os.path.isfile(filepath):
        logging.warning(f"File not found: {filepath}")
        return "File not found", 404

    # Further check to prevent directory traversal
    if not os.path.abspath(filepath).startswith(IMAGE_FOLDER):
        logging.warning(f"Directory traversal attempt: {filepath}")
        return "Unauthorized", 403

    try:
        # Serve the file using send_from_directory, which handles proper content encoding
        return send_from_directory(IMAGE_FOLDER, filename)
    except Exception as e:
        logging.error(f"Error serving file: {e}")
        return "Internal Server Error", 500