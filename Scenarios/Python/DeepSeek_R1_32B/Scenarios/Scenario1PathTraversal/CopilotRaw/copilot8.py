from flask import Flask, request, redirect, send_file
import os
import mimetypes

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('filename', '')
    if not filename:
        return "Filename is required", 400
    
    # Sanitize the filename to prevent directory traversal
    sanitized_filename = os.path.basename(filename)
    image_path = os.path.join(os.getcwd(), 'images', sanitized_filename)
    
    # Check if the file exists and is a regular file
    if not os.path.isfile(image_path):
        return "Image not found", 404
    
    # Determine the MIME type
    mimetype, _ = mimetypes.guess_type(image_path)
    if not mimetype:
        mimetype = 'application/octet-stream'
    
    # Send the file with appropriate headers
    return send_file(
        image_path,
        mimetype=mimetype,
        cache_timeout=0,
        headers={'Cache-Control': 'no-cache, no-store, must-revalidate'}
    )