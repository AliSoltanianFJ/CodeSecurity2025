from flask import Flask, request, jsonify, abort
from flask import escape as flask_escape
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename
import uuid

app = Flask(__name__)

# Security: limit upload size to 10 MB
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MiB

# Allowed extension set
ALLOWED_EXTENSIONS = {'.pdf'}

# Upload directory (kept inside the project, not a user-controlled path)
UPLOAD_DIR = os.path.join(os.path.dirname(__file__), 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)
# Restrict directory permissions
try:
    os.chmod(UPLOAD_DIR, 0o700)
except Exception:
    # If the OS doesn't allow changing permissions, continue without failing
    pass


def _is_pdf_file(file_stream) -> bool:
    """
    Check the file header to make sure this is a PDF file.
    We read a small prefix and then rewind the stream.
    """
    try:
        # Read enough bytes to check PDF header
        file_stream.seek(0)
        header = file_stream.read(8)
        file_stream.seek(0)
        if not header:
            return False
        return header.startswith(b'%PDF')
    except Exception:
        return False


@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    """
    Accepts a single file field named 'pdf' via multipart/form-data POST.
    Validates filename, extension, file magic header, size limit, and writes
    the file to a safe uploads directory with a generated unique name.
    Returns JSON with a non-sensitive stored filename.
    """
    # Ensure correct content type
    content_type = request.content_type or ''
    if 'multipart/form-data' not in content_type.lower():
        return jsonify({'error': 'Invalid content type'}), 400

    if 'pdf' not in request.files:
        return jsonify({'error': 'No file part named "pdf"'}), 400

    uploaded = request.files['pdf']

    # Ensure uploaded object has filename and content
    orig_filename = uploaded.filename or ''
    if not orig_filename:
        return jsonify({'error': 'No filename provided'}), 400

    # Sanitize filename and enforce extension
    safe_name = secure_filename(orig_filename)
    if not safe_name:
        return jsonify({'error': 'Invalid filename'}), 400

    _, ext = os.path.splitext(safe_name)
    ext = ext.lower()
    if ext not in ALLOWED_EXTENSIONS:
        return jsonify({'error': 'Invalid file extension'}), 400

    # Prevent overly long filenames
    if len(safe_name) > 255:
        return jsonify({'error': 'Filename too long'}), 400

    # Validate file magic/header to confirm PDF
    if not _is_pdf_file(uploaded.stream):
        return jsonify({'error': 'Uploaded file is not a valid PDF'}), 400

    # Generate a unique filename to avoid collisions and avoid exposing original name
    unique_name = f"{uuid.uuid4().hex}{ext}"
    dest_path = os.path.join(UPLOAD_DIR, unique_name)

    # Ensure destination is within our uploads directory
    if not os.path.commonpath([os.path.abspath(dest_path), os.path.abspath(UPLOAD_DIR)]) == os.path.abspath(UPLOAD_DIR):
        return jsonify({'error': 'Invalid destination path'}), 400

    # Write file in chunks to prevent large memory usage
    try:
        uploaded.stream.seek(0)
        with open(dest_path, 'wb') as out_f:
            while True:
                chunk = uploaded.stream.read(8192)
                if not chunk:
                    break
                out_f.write(chunk)
        # Restrict file permissions
        try:
            os.chmod(dest_path, 0o600)
        except Exception:
            pass
    except Exception:
        # Do not expose internal errors or paths to the client
        return jsonify({'error': 'Failed to save uploaded file'}), 500

    # Return only non-sensitive information
    return jsonify({'status': 'ok', 'stored_filename': unique_name}), 201


if __name__ == '__main__':
    # Do not enable debug mode in production
    app.run(host='127.0.0.1', port=5000, debug=False)
