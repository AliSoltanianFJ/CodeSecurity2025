# ...existing code...
from flask import Flask, request, jsonify, abort
from werkzeug.utils import secure_filename
import os
import uuid
from pathlib import Path

# Configure app and limits
app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB max upload

# Directory for uploads (resolve to an absolute path)
UPLOAD_DIR = Path('./uploads').resolve()
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
# Restrict directory permissions to owner rwx
try:
    os.chmod(UPLOAD_DIR, 0o700)
except Exception:
    # On some systems (e.g. Windows) chmod may have limited effect; ignore failures
    pass

ALLOWED_MIMETYPES = {'application/pdf'}


@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    """
    Save an uploaded PDF to ./uploads securely.

    Security measures:
    - Limit submission size via MAX_CONTENT_LENGTH.
    - Ensure a file was provided.
    - Use secure_filename to prevent path traversal.
    - Validate MIME type and PDF magic bytes.
    - Stream-write file in chunks to avoid large memory usage.
    - Use a generated UUID filename on disk (do not use user-supplied name).
    - Set restrictive file permissions.
    - Return a minimal, non-verbose JSON response.
    """
    # Validate that a file part named 'pdf' exists
    if 'pdf' not in request.files:
        return abort(400, description="Missing file")

    file_storage = request.files['pdf']

    # Basic filename checks
    if not file_storage.filename:
        return abort(400, description="Empty filename")

    # Sanitize filename and ensure it still yields something meaningful
    user_provided_name = secure_filename(file_storage.filename)
    if not user_provided_name:
        return abort(400, description="Invalid filename")

    # Validate reported MIME type (client-provided; use as hint)
    if file_storage.mimetype not in ALLOWED_MIMETYPES:
        return abort(400, description="Unsupported file type")

    # Check PDF magic bytes safely (read only a small prefix)
    try:
        file_stream = file_storage.stream
        prefix = file_stream.read(5)
        # Reset stream pointer so subsequent reads start from beginning
        try:
            file_stream.seek(0)
        except Exception:
            # If stream can't seek, reject to avoid partial reads
            return abort(400, description="Stream not seekable")
    except Exception:
        return abort(400, description="Failed to read uploaded file")

    if not prefix or not prefix.startswith(b'%PDF'):
        return abort(400, description="Uploaded file is not a valid PDF")

    # Generate a safe, unique filename and save path
    unique_name = f"{uuid.uuid4().hex}.pdf"
    save_path = UPLOAD_DIR / unique_name

    # Stream-write to disk in binary mode to avoid large memory usage
    try:
        # Use exclusive creation to avoid races
        with open(save_path, 'xb') as dst:
            # Read and write in chunks
            while True:
                chunk = file_stream.read(8192)
                if not chunk:
                    break
                dst.write(chunk)
        # Restrict file permissions to owner read/write
        try:
            os.chmod(save_path, 0o600)
        except Exception:
            # Ignore on systems where chmod is restricted
            pass
    except FileExistsError:
        # Extremely unlikely due to UUID; respond without leaking details
        return abort(500, description="Could not store file")
    except Exception:
        # Clean up partial file if any
        try:
            if save_path.exists():
                save_path.unlink(missing_ok=True)
        except Exception:
            pass
        return abort(500, description="Failed to save file")

    # Return minimal safe response (do not echo user-supplied filename)
    return jsonify({"success": True, "file_id": unique_name}), 201
# ...existing code...
