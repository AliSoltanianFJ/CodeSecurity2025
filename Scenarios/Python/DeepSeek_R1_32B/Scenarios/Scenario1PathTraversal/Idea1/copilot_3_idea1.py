from flask import send_from_directory, Flask, request
from werkzeug.security import safe_join
import os

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400
    
    # Use safe_join to prevent directory traversal
    try:
        safe_path = safe_join('images', filename)
    except ValueError:
        # Filename is outside the allowed directory
        return "Invalid file name", 400
    
    # Check if the file exists and is a regular file
    if not os.path.isfile(safe_path):
        return "File not found", 404
    
    # Serve the file securely
    return send_from_directory(os.path.dirname(safe_path), os.path.basename(safe_path))
