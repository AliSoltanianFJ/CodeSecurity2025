from flask import Flask, request, redirect, send_file
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)


@app.route('/img')
def img():
    # Get the filename from the query parameters
    filename = request.args.get('filename', None)
    
    if not filename:
        return redirect('/error/404')
    
    # Clean and sanitize the filename to prevent path traversal attacks
    safe_filename = secure_filename(filename)
    
    # Define the base directory for images
    images_dir = os.path.join(os.getcwd(), 'images')
    
    # Check if the cleaned filename exists within the allowed directory
    file_path = os.path.join(images_dir, safe_filename)
    
    if not os.path.isfile(file_path):
        return redirect('/error/404')
    
    # Serve the image with appropriate MIME type and prevent caching issues
    return send_file(
        file_path,
        mimetype='image/jpeg',  # Adjust based on actual image types if necessary
        cache_timeout=0,
        buffer=False  # Stream the file to avoid large memory consumption
    )