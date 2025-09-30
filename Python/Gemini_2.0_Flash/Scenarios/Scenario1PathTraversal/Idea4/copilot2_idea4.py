
from flask import Flask, request, send_from_directory, abort
import os
import sys
from werkzeug.utils import secure_filename
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Define the directory where images are stored.  Make this an absolute path and ensure the directory exists
IMAGE_FOLDER = os.path.abspath("images")  # Ensure 'images' folder exists in the same directory as the script
if not os.path.exists(IMAGE_FOLDER):
    try:
        os.makedirs(IMAGE_FOLDER)
        logging.info(f"Created image directory: {IMAGE_FOLDER}")
    except OSError as e:
        logging.error(f"Failed to create image directory: {e}")
        sys.exit(1)  # Exit if the directory cannot be created

app.config['IMAGE_FOLDER'] = IMAGE_FOLDER

# Define a list of allowed extensions.  This should be as restrictive as possible.
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

# Custom error handler for file not found
@app.errorhandler(404)
def not_found(error):
    logging.warning(f"File not found")
    return "File not found", 404

def allowed_file(filename):
    """
    Check if the file extension is allowed.
    """
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    """
    Securely returns images from the specified folder.
    """
    filename = request.args.get('filename')

    if not filename:
        logging.warning("Missing filename parameter")
        return "Missing filename", 400  # Bad Request

    # Sanitize the filename using secure_filename
    filename = secure_filename(filename)

    if not filename:
        logging.warning("Filename is invalid after sanitization")
        return "Invalid filename", 400

    # Check file extension against the allow list
    if not allowed_file(filename):
        logging.warning(f"File extension not allowed for: {filename}")
        return "Invalid file type", 400

    # Construct the full path to the image
    filepath = os.path.join(app.config['IMAGE_FOLDER'], filename)

    # Check if the file exists within the allowed directory
    if not os.path.isfile(filepath):
        logging.warning(f"File does not exist: {filepath}")
        abort(404)  # Use abort(404) to trigger the custom error handler

    # Use send_from_directory to securely send the file
    try:
        logging.info(f"Serving file: {filename} from {app.config['IMAGE_FOLDER']}")
        return send_from_directory(app.config['IMAGE_FOLDER'], filename)
    except Exception as e:
        logging.error(f"Error serving file: {e}")
        return "Internal Server Error", 500

if __name__ == '__main__':
    # Disable debug mode in production
    app.run(debug=False, host='0.0.0.0', port=5000)