from flask import Flask, request, jsonify, abort
from werkzeug.utils import secure_filename
import os
import uuid
import tempfile
from pathlib import Path

app = Flask(__name__)

# Limit upload size to 10 MiB
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB

# Directory to store uploads (private, not served directly)
BASE_UPLOAD_DIR = Path(__file__).parent / "uploads"
BASE_UPLOAD_DIR.mkdir(mode=0o700, parents=True, exist_ok=True)

ALLOWED_EXT = {'.pdf'}
MAGIC_PREFIX = b'%PDF-'  # PDF files start with "%PDF-"
FILENAME_MAX_LEN = 128
CHUNK_SIZE = 4096  # 4 KiB


@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Ensure request is multipart/form-data with a file part named 'pdf'
    if 'pdf' not in request.files:
        return jsonify({'error': 'No file part named "pdf"'}), 400

    file_storage = request.files['pdf']

    # Basic filename checks
    if not getattr(file_storage, 'filename', None):
        return jsonify({'error': 'No filename provided'}), 400

    original_filename = secure_filename(file_storage.filename)
    if not original_filename:
        return jsonify({'error': 'Invalid filename'}), 400

    if len(original_filename) > FILENAME_MAX_LEN:
        return jsonify({'error': 'Filename too long'}), 400

    # Validate extension
    ext = Path(original_filename).suffix.lower()
    if ext not in ALLOWED_EXT:
        return jsonify({'error': 'Only PDF files are allowed'}), 400

    # Check magic bytes to ensure content is likely a PDF.
    # Use stream read + seek to avoid loading whole file into memory.
    try:
        stream = file_storage.stream
        stream.seek(0)
        header = stream.read(len(MAGIC_PREFIX))
        stream.seek(0)
    except Exception:
        return jsonify({'error': 'Unable to read uploaded file'}), 400

    if header != MAGIC_PREFIX:
        return jsonify({'error': 'Uploaded file is not a valid PDF'}), 400

    # Create a secure, non-guessable filename to store (do not expose server path)
    file_id = uuid.uuid4().hex
    stored_name = f"{file_id}.pdf"
    target_path = BASE_UPLOAD_DIR / stored_name

    # Write atomically to a temporary file in the uploads directory
    try:
        with tempfile.NamedTemporaryFile(dir=str(BASE_UPLOAD_DIR), prefix=file_id + ".", suffix=".tmp", delete=False) as tmp:
            tmp_path = Path(tmp.name)
            # Copy in chunks to prevent large memory usage
            while True:
                chunk = stream.read(CHUNK_SIZE)
                if not chunk:
                    break
                tmp.write(chunk)
            tmp.flush()
            os.fsync(tmp.fileno())

        # Set strict permissions on the final file
        os.replace(str(tmp_path), str(target_path))
        os.chmod(str(target_path), 0o600)
    except Exception:
        # Ensure no temporary files leaked
        try:
            if 'tmp_path' in locals() and tmp_path.exists():
                tmp_path.unlink()
        except Exception:
            pass
        return jsonify({'error': 'Failed to save uploaded file'}), 500

    # Do not return server file system paths or any sensitive info.
    return jsonify({'status': 'uploaded', 'file_id': file_id}), 201
