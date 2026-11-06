from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    from flask import abort, Response
    import werkzeug.utils

    filename = request.args.get('file')
    if not filename:
        abort(400, description="No file specified")

    # Restrict allowed filename characters: only alphanumerics, dash, underscore, and dot.
    if not filename.replace('.', '').replace('-', '').replace('_', '').isalnum():
        abort(400, description="Invalid characters in file name")

    # Only allow specific image extensions
    allowed_exts = {'.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'}
    _, ext = os.path.splitext(filename)
    if ext.lower() not in allowed_exts:
        abort(400, description="Unsupported file type")

    # Use werkzeug.utils.secure_filename to sanitize filename
    safe_filename = werkzeug.utils.secure_filename(filename)
    image_dir = os.path.join(os.path.dirname(__file__), 'images')
    abs_image_path = os.path.join(image_dir, safe_filename)

    # Ensure file is within the images directory
    if not os.path.commonprefix([os.path.abspath(abs_image_path), os.path.abspath(image_dir)]) == os.path.abspath(image_dir):
        abort(400, description="Invalid file path")

    # Check if file exists and is a file
    if not os.path.isfile(abs_image_path):
        abort(404, description="File not found")

    # Open file in binary mode and read content
    try:
        with open(abs_image_path, 'rb') as f:
            data = f.read()
    except Exception:
        abort(500, description="Error reading image")

    # Set appropriate Content-Type header
    mimetypes = {
        '.png': 'image/png',
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.gif': 'image/gif',
        '.bmp': 'image/bmp',
        '.webp': 'image/webp'
    }
    mime = mimetypes.get(ext.lower(), 'application/octet-stream')

    headers = {
        'Content-Type': mime,
        'Content-Disposition': f'inline; filename="{safe_filename}"',
        'X-Content-Type-Options': 'nosniff'
    }

    return Response(data, headers=headers)