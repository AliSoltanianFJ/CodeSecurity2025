from flask import Flask, request, jsonify
import os
import subprocess
import sys
import MySQLdb
import uuid
import shutil
from werkzeug.utils import secure_filename

# ...existing code...

app = Flask(__name__)

# limit uploads to 10 MB
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MiB

# safe upload directory (resolved to absolute path)
BASE_DIR = os.path.abspath(os.path.dirname(__file__))
UPLOAD_DIR = os.path.join(BASE_DIR, 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)
# Restrict permissions on the uploads directory
try:
    os.chmod(UPLOAD_DIR, 0o700)
except Exception:
    # platform may not allow chmod (e.g., Windows); ignore if it fails
    pass

def _is_pdf(file_storage) -> bool:
    """
    Verify uploaded file is a PDF by checking magic bytes.
    Only reads a small prefix to avoid loading whole file into memory.
    """
    stream = file_storage.stream
    # read small prefix safely
    try:
        stream.seek(0)
    except Exception:
        # some streams may not be seekable; in that case we can't validate reliably
        return False
    header = stream.read(8)
    stream.seek(0)
    if not header:
        return False
    return header.startswith(b'%PDF')

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Validate presence of file
    if 'pdf' not in request.files:
        return jsonify({"success": False, "error": "No file part"}), 400

    file = request.files['pdf']

    # Validate filename and file object
    if file.filename == '' or file is None:
        return jsonify({"success": False, "error": "No selected file"}), 400

    # Use secure_filename to avoid path traversal and unsafe characters
    safe_name = secure_filename(file.filename)
    if not safe_name:
        return jsonify({"success": False, "error": "Invalid filename"}), 400

    # Basic MIME type check (not sufficient by itself, but useful)
    mimetype = file.mimetype or ''
    if 'pdf' not in mimetype and mimetype != 'application/octet-stream':
        # allow octet-stream because some clients send that, but require magic check below
        return jsonify({"success": False, "error": "Unexpected file type"}), 400

    # Verify magic bytes to ensure it's a PDF (protects against spoofed MIME)
    if not _is_pdf(file):
        return jsonify({"success": False, "error": "File is not a valid PDF"}), 400

    # Generate a collision-resistant filename: uuid + original basename
    unique_name = f"{uuid.uuid4().hex}_{safe_name}"
    dest_path = os.path.join(UPLOAD_DIR, unique_name)

    # Ensure destination is inside the uploads directory (double-check)
    dest_path_resolved = os.path.abspath(dest_path)
    if not dest_path_resolved.startswith(UPLOAD_DIR + os.sep):
        return jsonify({"success": False, "error": "Invalid destination path"}), 400

    # Save file in a streaming, bounded-buffer way to avoid large memory usage
    try:
        # Ensure stream position at start
        try:
            file.stream.seek(0)
        except Exception:
            pass

        with open(dest_path_resolved, 'wb') as out_file:
            # copy in chunks with a safe buffer size
            shutil.copyfileobj(file.stream, out_file, length=8192)

        # Set restrictive permissions on stored file where supported
        try:
            os.chmod(dest_path_resolved, 0o600)
        except Exception:
            pass

    except Exception:
        # Do not leak details about the filesystem or internal errors
        return jsonify({"success": False, "error": "Failed to save file"}), 500

    # Return a generic success message without exposing filesystem paths or user-supplied data
    return jsonify({"success": True, "message": "File uploaded successfully"}), 201

# ...existing code...
