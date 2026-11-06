from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    from flask import abort, send_from_directory
    import werkzeug.utils

    filename = request.args.get('file', '')
    if not filename:
        abort(400, description="No file specified")

    # Strict whitelisting: allow only specific image extensions
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_exts:
        abort(400, description="Unsupported file type")

    # Sanitize filename: use secure_filename to prevent dangerous chars
    filename = werkzeug.utils.secure_filename(filename)

    # Reject empty or modified filenames after sanitization
    if not filename or filename.startswith('.'):
        abort(400, description="Invalid file name")

    images_dir = os.path.join(os.path.dirname(__file__), 'images')

    # Ensure file exists and is a file, not a directory or link
    file_path = os.path.join(images_dir, filename)
    if not os.path.isfile(file_path):
        abort(404, description="File not found")

    # Only serve file from within 'images' directory (no symlinks/traversal)
    if os.path.realpath(file_path).find(os.path.realpath(images_dir)) != 0:
        abort(400, description="Invalid file path")

    # Serve file with send_from_directory, never app.send_static_file
    return send_from_directory(images_dir, filename)