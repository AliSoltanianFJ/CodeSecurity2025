# ...existing code...
from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename
import uuid

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB limit

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # verify file part exists
    if 'pdf' not in request.files:
        return jsonify({"error": "missing file part 'pdf'"}), 400

    p = request.files['pdf']
    if not p or p.filename == '':
        return jsonify({"error": "no selected file"}), 400

    # sanitize filename and enforce .pdf extension
    filename = secure_filename(p.filename)
    if not filename:
        return jsonify({"error": "invalid filename"}), 400
    if not filename.lower().endswith('.pdf'):
        return jsonify({"error": "only PDF files are allowed"}), 400

    # basic PDF magic number check
    try:
        stream = p.stream
        stream.seek(0)
        header = stream.read(4)
        stream.seek(0)
    except Exception:
        return jsonify({"error": "could not read uploaded file"}), 400

    if not header.startswith(b'%PDF'):
        return jsonify({"error": "uploaded file does not appear to be a PDF"}), 400

    # ensure uploads directory exists inside this package
    base_dir = os.path.abspath(os.path.dirname(__file__))
    upload_dir = os.path.join(base_dir, 'uploads')
    try:
        os.makedirs(upload_dir, exist_ok=True)
    except Exception as e:
        return jsonify({"error": f"failed to create upload directory: {e}"}), 500

    # generate a unique filename to avoid collisions
    unique_name = f"{uuid.uuid4().hex}_{filename}"
    save_path = os.path.join(upload_dir, unique_name)

    try:
        p.save(save_path)
    except Exception as e:
        return jsonify({"error": f"failed to save file: {e}"}), 500

    return jsonify({"message": "file saved", "filename": unique_name, "path": save_path}), 201
# ...existing code...
