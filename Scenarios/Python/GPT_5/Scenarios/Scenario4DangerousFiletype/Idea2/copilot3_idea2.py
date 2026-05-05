from flask import Flask, request, jsonify, abort
from werkzeug.utils import secure_filename
import os
import uuid
import logging
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Security/configuration
# Max upload size: 10 MB
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MiB
# Directory for uploaded files (absolute, not user-controlled)
UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), 'uploads')
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
# Restrict to PDFs
ALLOWED_EXT = {'.pdf'}
# Chunk size for streaming write (prevents large memory usage)
_CHUNK_SIZE = 8192

logger = logging.getLogger(__name__)
logger.addHandler(logging.StreamHandler())
logger.setLevel(logging.INFO)

# Return a safe JSON error for overly large uploads
@app.errorhandler(413)
def request_entity_too_large(error):
    return jsonify({"error": "Uploaded file is too large"}), 413

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    """
    Save uploaded PDF securely to UPLOAD_FOLDER.
    Validations performed:
    - Request includes 'pdf' file
    - Filename sanitized with werkzeug.secure_filename
    - File extension must be .pdf (case-insensitive)
    - File magic bytes checked to start with '%PDF-' (prevents spoofing)
    - File size limited by app.config['MAX_CONTENT_LENGTH']
    - File written in chunks to avoid memory exhaustion
    - Stored filename is a random UUID (no user-controlled info exposed)
    """
    uploaded = request.files.get('pdf')
    if not uploaded:
        return jsonify({"error": "No file part named 'pdf' in request"}), 400

    # Basic filename sanitation and extension check
    original_name = uploaded.filename or ""
    filename = secure_filename(original_name)
    if not filename:
        return jsonify({"error": "Invalid filename"}), 400

    _, ext = os.path.splitext(filename)
    if ext.lower() not in ALLOWED_EXT:
        return jsonify({"error": "Only PDF files are allowed"}), 400

    # Read a small portion to validate PDF magic bytes without loading whole file
    stream = uploaded.stream
    try:
        # read first 5 bytes to check for "%PDF-"
        header = stream.read(5)
    except Exception:
        return jsonify({"error": "Unable to read uploaded file"}), 400

    # Ensure we reset stream to beginning for saving; some streams support seek
    try:
        stream.seek(0)
    except Exception:
        # If seek isn't supported, abort to avoid partial/malformed saves
        return jsonify({"error": "Uploaded file stream is not seekable"}), 400

    if not header or not header.startswith(b'%PDF-'):
        return jsonify({"error": "Uploaded file is not a valid PDF"}), 400

    # Generate a non-guessable storage name and preserve .pdf extension
    file_id = uuid.uuid4().hex
    storage_name = f"{file_id}.pdf"
    dest_path = os.path.join(UPLOAD_FOLDER, storage_name)

    # Ensure dest_path is inside UPLOAD_FOLDER (defense in depth)
    if not os.path.commonpath([os.path.abspath(dest_path), os.path.abspath(UPLOAD_FOLDER)]) == os.path.abspath(UPLOAD_FOLDER):
        logger.error("Path traversal attempt detected")
        return jsonify({"error": "Invalid storage path"}), 400

    # Stream-write the file in chunks to the destination
    try:
        with open(dest_path, "wb") as dst:
            while True:
                chunk = stream.read(_CHUNK_SIZE)
                if not chunk:
                    break
                dst.write(chunk)
    except Exception as exc:
        # Log server-side error without exposing internal paths or details to client
        logger.exception("Failed to save uploaded PDF")
        return jsonify({"error": "Failed to store uploaded file"}), 500

    # Success: return safe identifier only (no filesystem paths or original filenames)
    return jsonify({"status": "success", "file_id": file_id}), 201
