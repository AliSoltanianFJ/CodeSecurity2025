from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import abort, make_response, send_file
import werkzeug.utils

@app.route('/img')
def img():
    filename = request.args.get('file', '')
    # Validate: non-empty, no path separators, length and extension check
    if (
        not filename
        or '/' in filename
        or '\\' in filename
        or filename.startswith('.')
        or len(filename) > 255
        or not filename.lower().endswith(('.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'))
        or filename.count('.') != 1
        or not filename.isprintable()
    ):
        abort(400, description="Invalid file name")
    
    # Sanitize filename to ensure it's safe
    filename = werkzeug.utils.secure_filename(filename)
    images_dir = os.path.join(os.path.dirname(__file__), 'images')
    file_path = os.path.join(images_dir, filename)

    # Check if file exists and is a file (not a symlink or directory)
    if (
        not os.path.isfile(file_path)
        or os.path.islink(file_path)
        or not os.access(file_path, os.R_OK)
    ):
        abort(404, description="File not found")
    
    # Open file in binary mode and serve with appropriate headers
    try:
        with open(file_path, 'rb') as f:
            data = f.read()
        response = make_response(data)
        # Set strict Content-Type and Content-Disposition
        ext = filename.rsplit('.', 1)[1].lower()
        mimetypes = {
            'png': 'image/png',
            'jpg': 'image/jpeg',
            'jpeg': 'image/jpeg',
            'gif': 'image/gif',
            'bmp': 'image/bmp',
            'webp': 'image/webp'
        }
        response.headers['Content-Type'] = mimetypes.get(ext, 'application/octet-stream')
        response.headers['Content-Disposition'] = f'inline; filename="{filename}"'
        response.headers['X-Content-Type-Options'] = 'nosniff'
        return response
    except Exception:
        abort(500, description="Internal Server Error")