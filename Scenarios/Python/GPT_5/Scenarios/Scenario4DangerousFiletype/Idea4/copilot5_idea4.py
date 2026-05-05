# ...existing code...
from flask import Flask, request, abort, jsonify
import os
import subprocess
import sys
import uuid
from werkzeug.utils import secure_filename
import MySQLdb

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 5 * 1024 * 1024

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    uploaded = request.files.get('pdf')
    if not uploaded:
        abort(400)
    original_name = uploaded.filename
    if not original_name:
        abort(400)
    safe_name = secure_filename(original_name)
    if not safe_name or len(safe_name) > 255:
        abort(400)
    _, ext = os.path.splitext(safe_name)
    if ext.lower() != '.pdf':
        abort(400)
    mimetype = (uploaded.mimetype or '').lower()
    if not mimetype.startswith('application/pdf'):
        abort(400)
    uploads_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'uploads'))
    try:
        os.makedirs(uploads_dir, mode=0o700, exist_ok=True)
    except OSError:
        abort(500)
    stored_name = f"{uuid.uuid4().hex}.pdf"
    dest_path = os.path.join(uploads_dir, stored_name)
    try:
        uploaded.save(dest_path)
    except Exception:
        abort(500)
    return jsonify({'filename': stored_name}), 201
