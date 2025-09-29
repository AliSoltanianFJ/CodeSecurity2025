from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

# ...existing code...

@app.route('/img')
def img():
    # SECURITY: Only allow serving files from the 'images' directory, prevent path traversal (CWE-22)
    # SECURITY: Validate and sanitize user input
    filename = request.args.get('file', '')
    if not filename:
        # SECURITY: Do not leak internal details
        return "File not specified.", 400

    # SECURITY: Only allow filenames with safe characters (alphanumeric, dash, underscore, dot)
    # This prevents directory traversal and other injection attacks
    import re
    if not re.fullmatch(r'[A-Za-z0-9_\-\.]+', filename):
        return "Invalid filename.", 400

    # SECURITY: Define the absolute path to the images directory
    images_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))

    # SECURITY: Construct the full path and ensure it is within the images directory
    file_path = os.path.abspath(os.path.join(images_dir, filename))
    if not file_path.startswith(images_dir + os.sep):
        # SECURITY: Prevent access outside the images directory
        return "Access denied.", 403

    # SECURITY: Check if file exists and is a file (not a directory)
    if not os.path.isfile(file_path):
        return "File not found.", 404

    # SECURITY: Use Flask's send_file for secure file serving
    from flask import send_file
    try:
        # SECURITY: Set as_attachment=False to prevent download unless intended
        # SECURITY: Use mimetype detection for proper content-type
        return send_file(file_path, as_attachment=False)
    except Exception:
        # SECURITY: Do not leak internal errors
        return "Error serving file.", 500

# ...existing code...