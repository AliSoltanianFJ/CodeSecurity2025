from flask import Flask, request, jsonify
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename
import uuid
from pathlib import Path

app = Flask(__name__)

# Limit upload size to 10 MiB to prevent buffer exhaustion
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB

# Directory to store uploads (relative to this file)
BASE_DIR = Path(__file__).resolve().parent
UPLOAD_DIR = BASE_DIR.joinpath('uploads')

# Ensure upload directory exists with safe permissions
UPLOAD_DIR.mkdir(mode=0o700, parents=True, exist_ok=True)


def _is_pdf_stream(stream):
    """
    Check the first bytes of the stream to verify PDF magic header.
    This function does not load the entire file into memory.
    """
    try:
        current_pos = None
        if hasattr(stream, 'tell') and hasattr(stream, 'seek'):
            current_pos = stream.tell()
            stream.seek(0)
        header = stream.read(5)
        # Reset stream position if possible
        if current_pos is not None:
            stream.seek(current_pos)
        return isinstance(header, (bytes, bytearray)) and header.startswith(b'%PDF-')
    except Exception:
        # If any error occurs during inspection, treat as non-PDF
        return False


@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    """
    Securely save an uploaded PDF to ./uploads.
    Validations performed:
    - Enforce POST method
    - Ensure 'pdf' file part exists
    - Ensure filename present
    - Validate file is PDF by magic bytes
    - Use a generated filename (UUID) to avoid path traversal and collisions
    - Save in chunks to avoid loading entire file into memory
    - Set restrictive file permissions on the saved file
    - Return safe JSON messages (no sensitive info)
    """
    # Ensure file part exists
    if 'pdf' not in request.files:
        return jsonify({"error": "No file part named 'pdf' in request"}), 400

    uploaded = request.files['pdf']

    # Ensure a filename was provided
    if not uploaded or uploaded.filename == '':
        return jsonify({"error": "No selected file"}), 400

    # Basic filename sanitization (we will not use the sanitized name for storage)
    original_name = secure_filename(uploaded.filename)
    if not original_name.lower().endswith('.pdf'):
        # Even if extension is wrong, still check magic header below; reject early to be strict
        return jsonify({"error": "Uploaded file does not have a .pdf extension"}), 400

    # Verify PDF magic header to prevent content spoofing
    if not _is_pdf_stream(uploaded.stream):
        return jsonify({"error": "Uploaded file is not a valid PDF"}), 400

    # Generate a safe, unique filename to avoid collisions and leaking original file names
    unique_name = f"{uuid.uuid4().hex}.pdf"
    dest_path = UPLOAD_DIR.joinpath(unique_name)

    # Prevent race-condition overwrite by using exclusive creation mode
    try:
        # Open destination in exclusive binary write mode
        # Write in chunks to avoid excessive memory use
        with open(dest_path, 'xb') as dst:
            # Reset the uploaded stream position to start
            try:
                uploaded.stream.seek(0)
            except Exception:
                # If stream is not seekable, we still attempt to read from current position
                pass

            chunk_size = 4096
            while True:
                chunk = uploaded.stream.read(chunk_size)
                if not chunk:
                    break
                # Write chunk - Python handles buffering safely
                dst.write(chunk)

        # Restrict permissions on the saved file (owner read/write only)
        try:
            os.chmod(dest_path, 0o600)
        except Exception:
            # If chmod fails (e.g., on some filesystems), ignore to avoid breaking upload
            pass

    except FileExistsError:
        # Extremely unlikely due to UUID, but handle gracefully
        return jsonify({"error": "A file with the generated name already exists, try again"}), 500
    except OSError:
        # Generic OS-level error (disk full, permission, etc.) - do not leak details
        return jsonify({"error": "Failed to save uploaded file"}), 500
    except Exception:
        # Catch-all for unexpected errors
        return jsonify({"error": "An unexpected error occurred"}), 500

    # Return minimal information to avoid revealing filesystem paths or other sensitive data
    return jsonify({"message": "File uploaded successfully", "file_id": unique_name}), 201
