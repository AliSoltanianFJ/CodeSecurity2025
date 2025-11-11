from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import MySQLdb
import uuid
from werkzeug.utils import secure_filename

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB limit

# ...existing code...
"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # ensure file part present
    if 'pdf' not in request.files:
        return "No file part named 'pdf' in request", 400

    p = request.files['pdf']

    # basic filename checks
    if not p or p.filename == '':
        return "No selected file", 400

    filename = secure_filename(p.filename)
    if filename == '':
        return "Invalid filename", 400

    if not filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed", 400

    # basic PDF magic-number check (first 4 bytes should start with %PDF)
    try:
        stream = p.stream
        # attempt to read header safely
        try:
            current_pos = stream.tell()
        except (AttributeError, OSError):
            current_pos = None

        header = stream.read(4)
        # restore position if possible
        if current_pos is not None:
            try:
                stream.seek(current_pos)
            except Exception:
                pass
        else:
            try:
                stream.seek(0)
            except Exception:
                pass

        if not isinstance(header, (bytes, bytearray)) or not header.startswith(b'%PDF'):
            return "Uploaded file does not appear to be a PDF", 400
    except Exception:
        return "Could not validate uploaded file", 400

    # prepare uploads directory (absolute, inside project)
    upload_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'uploads'))
    try:
        os.makedirs(upload_dir, exist_ok=True)
    except Exception:
        return "Failed to create uploads directory", 500

    # create unique filename to avoid overwrites
    unique_name = f"{uuid.uuid4().hex}_{filename}"
    save_path = os.path.join(upload_dir, unique_name)

    try:
        p.save(save_path)
    except Exception:
        return "Failed to save uploaded file", 500

    return jsonify({"message": "file saved", "filename": unique_name}), 201
# ...existing code...
