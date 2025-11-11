from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename
import uuid

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # basic request checks
    if 'pdf' not in request.files:
        return jsonify({"error": "no file part 'pdf' in request"}), 400

    p = request.files['pdf']
    if not p or p.filename == '':
        return jsonify({"error": "no selected file"}), 400

    # sanitize filename and require .pdf extension
    filename = secure_filename(p.filename or "")
    if filename == "" or not filename.lower().endswith('.pdf'):
        return jsonify({"error": "only PDF files are allowed"}), 400

    # quick magic-number check to reduce risk of non-PDF uploads
    try:
        stream = p.stream
        stream.seek(0)
        header = stream.read(4)
        stream.seek(0)
        if not isinstance(header, (bytes, bytearray)) or not header.startswith(b'%PDF'):
            return jsonify({"error": "uploaded file does not appear to be a PDF"}), 400
    except Exception:
        return jsonify({"error": "could not validate uploaded file"}), 400

    # enforce a maximum file size (e.g., 10 MB)
    MAX_BYTES = 10 * 1024 * 1024

    # ensure uploads directory exists (relative to this file)
    upload_dir = os.path.join(os.path.dirname(__file__), 'uploads')
    try:
        os.makedirs(upload_dir, exist_ok=True)
    except Exception as e:
        return jsonify({"error": f"failed to create upload directory: {e}"}), 500

    # create a unique, safe filename to avoid collisions and path traversal
    unique_name = f"{uuid.uuid4().hex}_{filename}"
    save_path = os.path.join(upload_dir, unique_name)

    # stream-write the file with size checking and set restrictive permissions
    try:
        with open(save_path, 'wb') as out:
            total = 0
            stream.seek(0)
            while True:
                chunk = stream.read(4096)
                if not chunk:
                    break
                total += len(chunk)
                if total > MAX_BYTES:
                    out.close()
                    try:
                        os.remove(save_path)
                    except Exception:
                        pass
                    return jsonify({"error": "file too large"}), 413
                out.write(chunk)
        try:
            os.chmod(save_path, 0o600)
        except Exception:
            # non-fatal: continue even if chmod fails on some filesystems
            pass
    except Exception as e:
        try:
            if os.path.exists(save_path):
                os.remove(save_path)
        except Exception:
            pass
        return jsonify({"error": f"failed to save file: {e}"}), 500

    return jsonify({"message": "file saved", "filename": unique_name}), 201
