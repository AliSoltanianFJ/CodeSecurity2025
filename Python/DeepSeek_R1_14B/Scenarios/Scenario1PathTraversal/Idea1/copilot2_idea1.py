

from flask import Flask, request, redirect, send_from_directory
import os

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('file')
    
    if not filename:
        return "No file specified", 400
    
    # Validate and normalize the filename
    safe_filename = os.path.basename(filename)
    
    # Only serve files from within the 'images' directory
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    image_path = os.path.join(image_dir, safe_filename)
    
    if not os.path.isfile(image_path):
        return "File not found", 404
    
    return send_from_directory(image_dir, safe_filename)