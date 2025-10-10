import re
from flask import Flask, request, redirect, abort, url_for
import os

app = Flask(__name__)
UPLOAD_FOLDER = 'images'

@app.route('/img')
def img():
    # Get filename from query parameter with default empty value
    filename = request.args.get('filename', '')

    # If no filename provided, return 400 Bad Request
    if not filename:
        abort(400, description="Filename is required")

    # Validate and sanitize filename to prevent directory traversal attacks
    allowed_characters = re.compile(r'^[a-zA-Z0-9-_\.]+$')  # Allowed characters: letters, numbers, underscore, hyphen, dot

    if not allowed_characters.match(filename):
        abort(400, description="Invalid filename")

    # Construct the full path using os.path.join to prevent directory traversal
    file_path = os.path.join(UPLOAD_FOLDER, filename)

    # Check if the file exists and is within the upload folder
    if not os.path.isfile(file_path) or not os.path.commonprefix([file_path, UPLOAD_FOLDER]):
        abort(404, description="File not found")

    # Return the image file with appropriate MIME type
    return redirect(url_for('static', filename=os.path.join('images', filename)), code=302)

