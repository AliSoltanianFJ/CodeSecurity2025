from flask import send_from_directory, request
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400
    
    # Sanitize the filename to prevent directory traversal
    safe_filename = secure_filename(filename)
    if not safe_filename:
        return "Invalid file name", 400
    
    # Construct the image path safely
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, safe_filename)
    
    # Check if the file exists and is within the images directory
    if not os.path.isfile(image_path) or not image_path.startswith(image_dir):
        return "File not found", 404
    
    # Serve the file securely
    return send_from_directory(image_dir, safe_filename)
