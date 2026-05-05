# ...existing code...
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
    # Expect a file field named 'pdf'
    if 'pdf' not in request.files:
        return jsonify({"error": "missing file part 'pdf'"}), 400

    p = request.files['pdf']
    if not p or p.filename == '':
        return jsonify({"error": "no selected file"}), 400

    # Normalize and validate filename
    filename = secure_filename(p.filename or "")
    if filename == "":
        return jsonify({"error": "invalid filename"}), 400
    if not filename.lower().endswith('.pdf'):
        return jsonify({"error": "only PDF files are allowed"}), 400

    # Enforce a reasonable max size (e.g. 10 MiB)
    MAX_SIZE = 10 * 1024 * 1024
    content_length = request.content_length
    if content_length is not None and content_length > MAX_SIZE:
        return jsonify({"error": "file too large"}), 413

    # Basic PDF magic-number check
    try:
        stream = p.stream
        stream.seek(0)
        header = stream.read(5)
        stream.seek(0)
        if not header.startswith(b'%PDF'):
            return jsonify({"error": "uploaded file does not appear to be a PDF"}), 400
    except Exception:
        return jsonify({"error": "could not validate uploaded file"}), 400

    # Ensure uploads directory is inside the package and exists
    upload_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'uploads'))
    try:
        os.makedirs(upload_dir, exist_ok=True)
    except Exception:
        return jsonify({"error": "failed to create upload directory"}), 500

    # Create a unique filename to avoid collisions
    unique_name = f"{uuid.uuid4().hex}_{filename}"
    save_path = os.path.join(upload_dir, unique_name)

    try:
        p.save(save_path)
    except Exception as e:
        return jsonify({"error": "failed to save file"}), 500

    return jsonify({"message": "file saved", "filename": unique_name, "path": save_path}), 201
