# ...existing code...
from flask import Flask, request, jsonify
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename
import uuid

app = Flask(__name__)
# limit uploads to 10 MB
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MiB

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # required file part
    if 'pdf' not in request.files:
        return jsonify({"error": "no file part 'pdf' in request"}), 400

    p = request.files['pdf']
    if not p or p.filename == '':
        return jsonify({"error": "no selected file"}), 400

    # sanitize filename and enforce .pdf extension
    filename = secure_filename(p.filename)
    if not filename.lower().endswith('.pdf'):
        return jsonify({"error": "only PDF files are allowed"}), 400

    # basic PDF magic number check (do not rely solely on this)
    try:
        stream = p.stream
        stream.seek(0)
        header = stream.read(4)
        stream.seek(0)
        if not isinstance(header, (bytes, bytearray)) or not header.startswith(b'%PDF'):
            return jsonify({"error": "uploaded file does not appear to be a PDF"}), 400
    except Exception:
        return jsonify({"error": "could not validate uploaded file"}), 400

    # create uploads directory next to this file
    upload_dir = os.path.join(os.path.dirname(__file__), 'uploads')
    try:
        os.makedirs(upload_dir, exist_ok=True)
    except Exception as e:
        return jsonify({"error": f"failed to create upload directory: {e}"}), 500

    # make filename unique to avoid overwriting and avoid using user-provided paths
    unique_name = f"{uuid.uuid4().hex}_{filename}"
    save_path = os.path.join(upload_dir, unique_name)

    try:
        p.save(save_path)
    except Exception as e:
        return jsonify({"error": f"failed to save file: {e}"}), 500

    return jsonify({"message": "file saved", "filename": unique_name, "path": save_path}), 201

# ...existing code...
