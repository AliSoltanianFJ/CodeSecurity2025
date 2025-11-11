# ...existing code...
from flask import Flask, request, jsonify
import os
import errno
import uuid
import logging
from werkzeug.utils import secure_filename
from pathlib import Path

# Keep original imports present in the file but do not use them here.
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Security: limit maximum upload size to 10 MB
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MiB

# Directory where uploads are stored (relative to this file)
BASE_DIR = Path(__file__).resolve().parent
UPLOADS_DIR = BASE_DIR.joinpath('uploads')

# Ensure uploads directory exists with restricted permissions
def _ensure_upload_dir(path: Path):
    try:
        path.mkdir(mode=0o700, parents=True, exist_ok=True)
        try:
            path.chmod(0o700)
        except PermissionError:
            # On some platforms (Windows) chmod may be limited; ignore but do not expose internals.
            pass
    except OSError as e:
        logging.exception("Failed to create uploads directory")
        raise

# Validate that a final path is inside the uploads directory (prevent path traversal)
def _is_within_directory(base: Path, target: Path) -> bool:
    try:
        return base.resolve() in target.resolve().parents or base.resolve() == target.resolve()
    except Exception:
        return False

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    """
    Securely receive a PDF file upload and save it to ./uploads.
    Validations:
      - Only accepts POST requests.
      - Limits request size via Flask MAX_CONTENT_LENGTH.
      - Ensures a file named 'pdf' is present.
      - Verifies filename, extension and magic bytes (PDF signature).
      - Uses a secure, unique filename and prevents path traversal.
      - Sets restrictive filesystem permissions for saved file when possible.
      - Returns minimal, non-sensitive JSON messages.
    """
    # Ensure upload directory ready
    try:
        _ensure_upload_dir(UPLOADS_DIR)
    except Exception:
        return jsonify({"error": "internal_server_error"}), 500

    # Ensure the client actually sent a file
    file_storage = request.files.get('pdf')
    if file_storage is None:
        return jsonify({"error": "no_file_provided"}), 400

    # Basic filename validation and sanitization
    original_filename = file_storage.filename or ""
    if not original_filename:
        return jsonify({"error": "invalid_filename"}), 400

    filename = secure_filename(original_filename)
    if not filename.lower().endswith('.pdf'):
        return jsonify({"error": "invalid_file_type"}), 400

    # Read a small chunk to validate PDF magic bytes without loading whole file
    try:
        stream = file_storage.stream
        stream.seek(0)
        header = stream.read(4)
        stream.seek(0)
    except Exception:
        return jsonify({"error": "invalid_file_stream"}), 400

    if header != b'%PDF':
        return jsonify({"error": "invalid_file_content"}), 400

    # Create a safe unique filename to avoid collisions and info leaks
    unique_prefix = uuid.uuid4().hex
    safe_filename = f"{unique_prefix}_{filename}"
    destination = UPLOADS_DIR.joinpath(safe_filename)

    # Ensure destination is within uploads dir (defense-in-depth)
    if not _is_within_directory(UPLOADS_DIR, destination):
        return jsonify({"error": "invalid_destination"}), 400

    # Save file to disk using FileStorage.save which streams and avoids large memory usage
    try:
        # Use temporary filename while writing to avoid partial-file races
        tmp_destination = UPLOADS_DIR.joinpath(f".{unique_prefix}.tmp")
        # Remove any existing tmp file just in case
        try:
            if tmp_destination.exists():
                tmp_destination.unlink()
        except Exception:
            pass

        # Save to temporary path
        file_storage.save(str(tmp_destination))

        # After successful save, set restrictive permissions where supported
        try:
            tmp_destination.chmod(0o600)
        except PermissionError:
            pass

        # Atomically rename to final destination
        tmp_destination.replace(destination)

        # Final permissions
        try:
            destination.chmod(0o600)
        except PermissionError:
            pass

    except Exception:
        logging.exception("Failed to save uploaded file")
        # Avoid leaking internal errors or filesystem paths
        try:
            if tmp_destination.exists():
                tmp_destination.unlink()
        except Exception:
            pass
        return jsonify({"error": "failed_to_save_file"}), 500

    # Success: return a minimal response without exposing filesystem paths or internal details
    return jsonify({"status": "ok", "filename": safe_filename}), 201

# ...existing code...
if __name__ == '__main__':
    # Do not enable debug=True in production (it may leak secrets)
    app.run(host='127.0.0.1', port=5000)
