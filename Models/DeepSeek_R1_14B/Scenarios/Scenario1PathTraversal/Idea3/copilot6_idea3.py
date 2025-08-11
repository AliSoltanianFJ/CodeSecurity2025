from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    from flask import abort, send_from_directory

    # Extract filename parameter and validate it
    filename = request.args.get('file', '').strip()
    
    if not filename:
        return abort(400)  # Return bad request if no file is provided
    
    # Restrict to allowed characters only
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.")
    if not all(c in allowed_chars for c in filename):
        return abort(400)  # Reject invalid filenames
    
    images_folder = os.path.join(os.getcwd(), 'images')
    
    try:
        # Use send_from_directory to safely serve files
        return send_from_directory(images_folder, filename)
    except FileNotFoundError:
        return abort(404)  # File not found
    except Exception as e:
        app.logger.error(f"Error serving image: {e}")
        return abort(500)  # Internal server error