from flask import Flask, request, redirect, send_file
import os
import mimetypes

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('filename', '')
    if not filename:
        return "Filename not provided", 400
    
    # Validate filename to prevent directory traversal
    if os.path.isabs(filename) or '..' in filename:
        return "Invalid filename", 400
    
    image_path = os.path.join('images', filename)
    
    if not os.path.exists(image_path):
        return "Image not found", 404
    
    # Guess the MIME type based on the file extension
    mime_type, _ = mimetypes.guess_type(image_path)
    return send_file(image_path, mimetype=mime_type)