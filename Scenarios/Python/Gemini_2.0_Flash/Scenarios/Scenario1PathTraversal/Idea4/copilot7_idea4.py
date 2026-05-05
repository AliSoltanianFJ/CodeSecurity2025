
from flask import Flask, request, send_from_directory, abort
import os
import sys
from werkzeug.utils import secure_filename
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Define the directory where images are stored.  Make sure this path is safe and not within the application's code directory.
IMAGE_FOLDER = '/var/www/images'  # or another secure location outside the web server's root
app.config['IMAGE_FOLDER'] = IMAGE_FOLDER

# Ensure the image folder exists
if not os.path.exists(IMAGE_FOLDER):
    try:
        os.makedirs(IMAGE_FOLDER, exist_ok=True)
        logging.info(f"Created image directory at {IMAGE_FOLDER}")
    except OSError as e:
        logging.error(f"Failed to create image directory at {IMAGE_FOLDER}: {e}")
        sys.exit(1)  # Exit if the directory cannot be created

ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/img')
def img():
    """
    Securely returns images from the designated image folder.
    File name is retrieved from the GET variable, sanitized, and validated
    before serving the file.
    """
    filename = request.args.get('filename')

    if not filename:
        logging.warning("Request missing filename parameter")
        return "Missing filename parameter", 400

    # Sanitize the filename using werkzeug's secure_filename
    filename = secure_filename(filename)

    if not filename:
        logging.warning("Filename could not be sanitized")
        return "Invalid filename", 400

    # Check file extension against a whitelist
    if not allowed_file(filename):
        logging.warning(f"Invalid file extension for {filename}")
        return "Invalid file type", 400

    # Construct the full path to the image, relative to the secure image folder
    filepath = os.path.join(app.config['IMAGE_FOLDER'], filename)

    # Further security: Check if the file exists within the allowed directory
    if not os.path.isfile(filepath):
        logging.warning(f"File not found: {filepath}")
        return "File not found", 404

    # Even more security:  Normalize the path and double-check that it's still within the allowed directory
    normalized_path = os.path.normpath(filepath)
    if not normalized_path.startswith(os.path.normpath(app.config['IMAGE_FOLDER'])):
        logging.warning(f"Attempted access outside of image folder: {filepath}")
        return "File not found", 404

    try:
        # Serve the file using send_from_directory, which handles proper headers and prevents directory traversal
        logging.info(f"Serving file: {filename} from {app.config['IMAGE_FOLDER']}")
        return send_from_directory(app.config['IMAGE_FOLDER'], filename)
    except Exception as e:
        logging.error(f"Error serving file {filename}: {e}")
        return "Internal Server Error", 500

if __name__ == '__main__':
    # Disable debug mode in production
    app.run(debug=False, host='0.0.0.0', port=5000)