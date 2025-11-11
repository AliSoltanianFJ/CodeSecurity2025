from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Configuration: maximum upload size (10 MiB)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB
UPLOAD_DIR = os.path.join(app.root_path, 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)


def _is_pdf_header(b: bytes) -> bool:
    return isinstance(b, (bytes, bytearray)) and b.startswith(b'%PDF-')


def _safe_join(base: str, *paths: str) -> str:
    """
    Join paths and ensure the result is inside base directory to prevent path traversal.
    """
    final_path = os.path.normpath(os.path.join(base, *paths))
    base_norm = os.path.normpath(base)
    # os.path.commonpath will raise on different drives on Windows; handle gracefully
    try:
        common = os.path.commonpath([base_norm, final_path])
    except ValueError:
        return None
    return final_path if common == base_norm else None


@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # 1) Validate request contains file
    uploaded = request.files.get('pdf')
    if uploaded is None:
        return jsonify({"error": "Missing file field 'pdf'"}), 400

    # 2) Validate filename and sanitize
    raw_filename = uploaded.filename or ""
    filename = secure_filename(raw_filename)
    if not filename:
        return jsonify({"error": "Invalid or empty filename"}), 400

    # 3) Enforce extension check (case-insensitive)
    _, ext = os.path.splitext(filename)
    if ext.lower() != '.pdf':
        return jsonify({"error": "Only .pdf files are allowed"}), 400

    # 4) Build destination path and ensure it is inside uploads directory
    dest_path = _safe_join(UPLOAD_DIR, filename)
    if dest_path is None:
        return jsonify({"error": "Invalid filename or attempted path traversal"}), 400

    # 5) Read initial bytes to validate PDF header without loading entire file into memory
    stream = uploaded.stream
    try:
        # Read header (small amount)
        header = stream.read(8)
        if not _is_pdf_header(header):
            return jsonify({"error": "Uploaded file is not a valid PDF (invalid header)"}), 400

        # Prepare to write file in chunks; track total size to enforce limits
        max_bytes = app.config['MAX_CONTENT_LENGTH']
        total_written = 0

        # Ensure parent directory exists (already created at module import, but double-check)
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)

        # Write header and remaining data in chunks to disk to avoid memory pressure
        with open(dest_path, 'wb') as out_f:
            out_f.write(header)
            total_written += len(header)

            # Read remainder in small chunks
            chunk_size = 8192
            while True:
                chunk = stream.read(chunk_size)
                if not chunk:
                    break
                out_f.write(chunk)
                total_written += len(chunk)
                if total_written > max_bytes:
                    # Exceeded allowed size, abort and remove partial file
                    out_f.close()
                    try:
                        os.remove(dest_path)
                    except Exception:
                        pass
                    return jsonify({"error": "File exceeds maximum allowed size"}), 413

        # 6) Additional simple sanity check: ensure file ends with PDF EOF marker
        try:
            with open(dest_path, 'rb') as f:
                # read last up to 1024 bytes to search for EOF marker
                f.seek(0, os.SEEK_END)
                size = f.tell()
                tail_read = 1024 if size >= 1024 else size
                f.seek(size - tail_read)
                tail = f.read(tail_read)
                if b'%%EOF' not in tail:
                    # Not a trustworthy PDF; remove file and reject
                    try:
                        os.remove(dest_path)
                    except Exception:
                        pass
                    return jsonify({"error": "Uploaded PDF appears malformed (missing EOF)"}), 400
        except Exception:
            # On unexpected filesystem errors, remove file if present and return generic error
            try:
                os.remove(dest_path)
            except Exception:
                pass
            return jsonify({"error": "Server error while validating uploaded file"}), 500

        # 7) Set secure file permissions where applicable (POSIX). On Windows this is best-effort.
        try:
            os.chmod(dest_path, 0o600)
        except Exception:
            # Failing to set permissions shouldn't expose details to client
            pass

        # 8) Success: do not expose internal full path. Return a safe, minimal acknowledgment.
        return jsonify({"message": "File uploaded successfully", "filename": filename}), 201

    except Exception:
        # Always fail closed and do not leak internal errors to the client
        try:
            if os.path.exists(dest_path):
                os.remove(dest_path)
        except Exception:
            pass
        return jsonify({"error": "Failed to process upload"}), 500


if __name__ == '__main__':
    # Do not enable debug mode in production (would leak server info)
    app.run(host='127.0.0.1', port=5000)
