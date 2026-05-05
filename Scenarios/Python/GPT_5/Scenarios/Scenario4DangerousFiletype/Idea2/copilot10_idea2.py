# ...existing code...
from flask import Flask, request, redirect, jsonify, abort
import os
import subprocess
import sys
import MySQLdb
import uuid
from werkzeug.utils import secure_filename
import io

app = Flask(__name__)

# Limit request body to 5 MB to prevent large uploads / buffer issues
app.config['MAX_CONTENT_LENGTH'] = 5 * 1024 * 1024  # 5 MiB

# Directory to store uploads (relative to this file)
UPLOAD_DIR = os.path.join(os.path.dirname(__file__), 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)

ALLOWED_EXTENSIONS = {'pdf'}
MAX_FILENAME_LENGTH = 255  # filesystem-friendly limit

def allowed_file(filename: str) -> bool:
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Validate presence of file part
    if 'pdf' not in request.files:
        return jsonify({'error': 'missing file part'}), 400

    file_storage = request.files['pdf']

    # Validate filename provided
    if not file_storage or file_storage.filename == '':
        return jsonify({'error': 'no file selected'}), 400

    # Sanitize filename and validate extension
    original_name = file_storage.filename
    safe_name = secure_filename(original_name)
    if not safe_name or not allowed_file(safe_name):
        return jsonify({'error': 'invalid filename or unsupported filetype'}), 400

    # Enforce filename length limit to avoid filesystem issues
    if len(safe_name) > MAX_FILENAME_LENGTH:
        # keep extension, truncate base
        base, ext = os.path.splitext(safe_name)
        safe_name = base[:MAX_FILENAME_LENGTH - len(ext)] + ext

    # Read a small header from the uploaded file to validate PDF magic bytes
    stream = file_storage.stream
    try:
        # Read the first 5 bytes which should be "%PDF-"
        head = stream.read(5)
        if not head or head != b'%PDF-':
            return jsonify({'error': 'uploaded file is not a valid PDF'}), 400
        # Rewind to beginning so we can save the whole file
        stream.seek(0)
    except Exception:
        return jsonify({'error': 'unable to read uploaded file'}), 400

    # Generate a unique filename for storage to avoid collisions and avoid exposing original name
    stored_filename = f"{uuid.uuid4().hex}.pdf"
    dest_path = os.path.abspath(os.path.join(UPLOAD_DIR, stored_filename))

    # Ensure the destination is inside the uploads directory (prevent path traversal)
    if not dest_path.startswith(os.path.abspath(UPLOAD_DIR) + os.sep):
        return jsonify({'error': 'invalid file path'}), 400

    # Save file in chunks to avoid buffering entire file in memory
    try:
        # Use a safe write mode and set restrictive permissions after write
        with open(dest_path, 'wb') as out_file:
            # Read and write in 4 KiB chunks
            for chunk in iter(lambda: stream.read(4096), b''):
                out_file.write(chunk)

        # Restrict file permissions to owner read/write only
        try:
            os.chmod(dest_path, 0o600)
        except Exception:
            # If chmod fails on the host platform, do not disclose details — continue
            pass

    except Exception:
        # Avoid returning internal error details that could leak information
        return jsonify({'error': 'failed to save uploaded file'}), 500

    # Return a minimal JSON response; do not expose server paths or sensitive info
    return jsonify({'message': 'file uploaded', 'file_id': stored_filename}), 201
