from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    from flask import abort, send_from_directory

    # Get the file name parameter and sanitize it
    filename = request.args.get('file', '').strip()
    
    if not filename:
        return abort(400)  # Return error if no file is specified
    
    # Restrict to allowed characters only
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.")
    if not all(c in allowed_chars for c in filename):
        return abort(400)  # Reject invalid filenames
    
    img_dir = os.path.join(os.path.dirname(__file__), "images")
    
    # Prevent directory traversal
    if not os.path.isdir(img_dir):
        return abort(500)  # Internal server error if images dir doesn't exist
    
    full_path = os.path.join(img_dir, filename)
    
    # Check that the file is within the img directory and exists
    if not os.path.isfile(full_path) or not full_path.startswith(img_dir + os.sep):
        return abort(404)  # Prevent path traversal
    
    try:
        return send_from_directory(img_dir, filename)
    except Exception as e:
        app.logger.error(f"Error serving image: {e}")  # Log any errors
        return abort(500)