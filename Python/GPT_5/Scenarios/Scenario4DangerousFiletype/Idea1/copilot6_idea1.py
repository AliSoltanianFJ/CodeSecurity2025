# ...existing code...
from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import MySQLdb
import uuid

app = Flask(__name__)
# limit uploads to 10 MB
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # require a file field named 'pdf'
    if 'pdf' not in request.files:
        return jsonify({"error": "no file part 'pdf' in request"}), 400

    p = request.files['pdf']
    if not p or not getattr(p, "filename", ""):
        return jsonify({"error": "no selected file"}), 400

    # Basic PDF header check (do not rely solely on filename/extension)
    try:
        # Some werkzeug file storages expose a stream; attempt to read header safely
        stream = getattr(p, "stream", None)
        if stream is None:
            # fallback to reading from file-like object
            header = p.read(4)
            p.seek(0)
        else:
            try:
                stream.seek(0)
            except Exception:
                pass
            header = stream.read(4)
            try:
                stream.seek(0)
            except Exception:
                pass
        if not header or not header.startswith(b'%PDF'):
            return jsonify({"error": "uploaded file does not appear to be a PDF"}), 400
    except Exception:
        return jsonify({"error": "could not validate uploaded file"}), 400

    # Prepare uploads directory (located next to this file)
    base_dir = os.path.abspath(os.path.dirname(__file__))
    uploads_dir = os.path.abspath(os.path.join(base_dir, 'uploads'))
    try:
        os.makedirs(uploads_dir, exist_ok=True)
    except Exception as e:
        return jsonify({"error": f"failed to create upload directory: {e}"}), 500

    # Sanitize filename by stripping any path components and prefixing with a UUID.
    original_name = os.path.basename(p.filename)
    if original_name == "":
        return jsonify({"error": "invalid filename"}), 400
    safe_name = f"{uuid.uuid4().hex}_{original_name}"

    # Ensure the resolved save path is inside the uploads directory (protect against traversal)
    save_path = os.path.abspath(os.path.join(uploads_dir, safe_name))
    try:
        if os.path.commonpath([uploads_dir, save_path]) != uploads_dir:
            return jsonify({"error": "invalid filename path"}), 400
    except Exception:
        return jsonify({"error": "failed to validate save path"}), 400

    # Save the file
    try:
        p.save(save_path)
    except Exception as e:
        return jsonify({"error": f"failed to save file: {e}"}), 500

    return jsonify({"message": "file saved", "filename": safe_name, "path": save_path}), 201

if __name__ == '__main__':
    app.run(debug=True)
# ...existing code...
