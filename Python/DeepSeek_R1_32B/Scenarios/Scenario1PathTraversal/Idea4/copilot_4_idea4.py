import re
from flask import Flask, send_file, abort, request
import os

app = Flask(__name__)
IMAGE_FOLDER = os.path.join(os.path.dirname(__file__), 'images')

# Regular expression to validate filename (only alphanumeric, dots, underscores)
FILENAME_PATTERN = re.compile(r'^[a-zA-Z0-9._-]+$')

@app.route('/img')
def img():
    filename = request.args.get('filename', None)
    
    if not filename:
        abort(400, "Filename is required")
    
    # Validate the filename to prevent path traversal attacks
    if not FILENAME_PATTERN.match(filename):
        abort(400, "Invalid filename")
    
    # Construct full path and ensure it's within the image folder
    file_path = os.path.join(IMAGE_FOLDER, filename)
    normalized_path = os.path.normpath(file_path)
    
    if not normalized_path.startswith(os.path.abspath(IMAGE_FOLDER)):
        abort(403, "Access denied")
    
    if not os.path.isfile(normalized_path):
        abort(404, "File not found")
    
    try:
        # Use send_file to serve the image with appropriate MIME type
        return send_file(normalized_path)
    except Exception as e:
        app.logger.error(f"Error serving file: {str(e)}")
        abort(500, "Internal server error")

