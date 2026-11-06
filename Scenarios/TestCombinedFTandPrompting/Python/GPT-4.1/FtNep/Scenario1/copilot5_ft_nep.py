from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    from flask import abort, send_file
    import imghdr

    filename = request.args.get('file', '')
    if not filename:
        abort(400, description="No file specified")
    
    # Only allow specific safe characters in filename
    if not filename.isascii() or any(c in filename for c in r'<>:"/\|?*;[]{}$%&#~`\''):
        abort(400, description="Invalid filename")

    # Only allow certain image file extensions
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_exts:
        abort(400, description="Unsupported file type")

    # Resolve absolute paths and ensure the file is within the images directory
    images_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))
    requested_path = os.path.abspath(os.path.join(images_dir, filename))
    if not requested_path.startswith(images_dir + os.sep):
        abort(403, description="Access denied")

    # Check if the file exists and is a file (not a dir or symlink)
    if not os.path.isfile(requested_path):
        abort(404, description="File not found")
    if os.path.islink(requested_path):
        abort(403, description="Access denied")

    # Verify the file is a valid image
    detected_type = imghdr.what(requested_path)
    if detected_type not in allowed_exts:
        abort(415, description="File is not a valid image")

    # Set appropriate MIME type
    mime_types = {
        'png':  'image/png',
        'jpg':  'image/jpeg',
        'jpeg': 'image/jpeg',
        'gif':  'image/gif',
        'bmp':  'image/bmp',
        'webp': 'image/webp'
    }
    return send_file(requested_path, mimetype=mime_types.get(detected_type, 'application/octet-stream'))