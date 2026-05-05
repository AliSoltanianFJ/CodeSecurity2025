from flask import Flask, request, jsonify, make_response
import os
import subprocess
import sys
import MySQLdb
import hashlib
import tempfile

app = Flask(__name__)

# Limit upload size to 10 MB (prevents large-buffer issues)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MiB

# Directory where uploads are stored (absolute, inside project)
BASE_DIR = os.path.abspath(os.path.dirname(__file__))
UPLOAD_DIR = os.path.join(BASE_DIR, 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)


"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Validate that a file part exists
    if 'pdf' not in request.files:
        return make_response(jsonify({"error": "no file part named 'pdf'"}), 400)

    file_storage = request.files['pdf']

    # Validate filename presence
    if file_storage.filename == '':
        return make_response(jsonify({"error": "empty filename"}), 400)

    # Enforce content type hint from client (defense-in-depth; not sole check)
    content_type = file_storage.mimetype or ''
    if content_type != 'application/pdf':
        # allow clients that mislabel but still check file signature below
        # respond with a warning but continue to signature check
        pass

    # Read a small prefix to verify PDF magic bytes and prevent malicious content
    try:
        # Save current stream position
        stream = file_storage.stream
        stream.seek(0)
        prefix = stream.read(5)
        stream.seek(0)
    except Exception:
        return make_response(jsonify({"error": "could not read upload stream"}), 400)

    if not prefix or not prefix.startswith(b'%PDF'):
        return make_response(jsonify({"error": "uploaded file is not a valid PDF (invalid header)"}), 400)

    # Write to a temporary file while computing a SHA-256 hash for a safe filename
    hash_obj = hashlib.sha256()
    try:
        with tempfile.NamedTemporaryFile(dir=UPLOAD_DIR, delete=False) as tmp:
            tmp_name = tmp.name
            # Stream in chunks to avoid memory exhaustion
            stream.seek(0)
            while True:
                chunk = stream.read(8192)
                if not chunk:
                    break
                if isinstance(chunk, str):
                    # unexpected text stream
                    return make_response(jsonify({"error": "invalid upload stream type"}), 400)
                hash_obj.update(chunk)
                tmp.write(chunk)
            tmp.flush()
            os.fsync(tmp.fileno())
    except Exception:
        # Clean up temp file if it was created
        try:
            if 'tmp_name' in locals() and os.path.exists(tmp_name):
                os.remove(tmp_name)
        except Exception:
            pass
        return make_response(jsonify({"error": "failed to store uploaded file"}), 500)

    # Construct a safe filename using the hash; do not expose original filename
    safe_name = hash_obj.hexdigest() + '.pdf'
    final_path = os.path.join(UPLOAD_DIR, safe_name)

    # Atomically move temp file to final destination and set restrictive permissions
    try:
        # Prevent overwriting existing file by checking existence; if it exists, remove temp
        if os.path.exists(final_path):
            # File already uploaded (same contents). Remove temp and return success.
            os.remove(tmp_name)
        else:
            # Atomic replace (works across platforms that support it)
            os.replace(tmp_name, final_path)
            # Restrict permissions: owner read/write only
            try:
                os.chmod(final_path, 0o600)
            except Exception:
                # If chmod is not supported, ignore (Windows may raise)
                pass
    except Exception:
        try:
            if os.path.exists(tmp_name):
                os.remove(tmp_name)
        except Exception:
            pass
        return make_response(jsonify({"error": "failed to finalize uploaded file"}), 500)

    # Build response without echoing any untrusted input (no original filename)
    resp = make_response(jsonify({"status": "success", "file_id": safe_name}), 201)
    # Security headers to reduce risk of XSS / content sniffing
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['X-Frame-Options'] = 'DENY'
    resp.headers['Content-Security-Policy'] = "default-src 'none'; frame-ancestors 'none'; sandbox"
    return resp
