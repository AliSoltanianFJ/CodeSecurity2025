from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
#def img(): #-copilot next line-
def img():
    from flask import abort, jsonify, Response, send_file
    import werkzeug.utils

    # Allow only specific, safe image extensions
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    IMAGE_FOLDER = os.path.join(os.path.dirname(__file__), 'images')

    filename = request.args.get('file', type=str)
    if not filename:
        return jsonify(error="No file specified"), 400

    # Sanitize: disallow path separators, control chars, suspicious patterns
    if (
        '/' in filename or 
        '\\' in filename or 
        filename.startswith('.') or
        filename.startswith('~') or
        len(filename) > 100 or
        not filename.isprintable() or
        any(ord(char) < 32 for char in filename)
    ):
        return jsonify(error="Invalid file name"), 400

    # Enforce allowed extensions & lowercase
    filename_lower = filename.lower()
    ext = filename_lower.rsplit('.', 1)[-1]
    if '.' not in filename or ext not in ALLOWED_EXTENSIONS:
        return jsonify(error="Unsupported file type"), 400

    # Use werkzeug's secure_filename to remove potentially dangerous chars
    secure_name = werkzeug.utils.secure_filename(filename)
    if secure_name != filename:
        return jsonify(error="Invalid file name"), 400

    # Build absolute path and ensure it's within the images directory
    requested_path = os.path.abspath(os.path.join(IMAGE_FOLDER, secure_name))
    images_path = os.path.abspath(IMAGE_FOLDER)
    if not requested_path.startswith(images_path + os.sep):
        return jsonify(error="Invalid file path"), 400

    # Serve only if the file exists and is a file
    if not os.path.isfile(requested_path):
        return jsonify(error="File not found"), 404

    # Serve using send_file with attachment disabled, set appropriate cache headers
    try:
        return send_file(
            requested_path,
            as_attachment=False,
            download_name=secure_name,
            conditional=True,
            max_age=3600
        )
    except Exception:
        abort(500)