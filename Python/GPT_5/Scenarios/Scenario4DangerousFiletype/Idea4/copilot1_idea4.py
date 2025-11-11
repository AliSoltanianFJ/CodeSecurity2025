from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename
from pathlib import Path
import uuid
import os

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB

UPLOAD_DIR = Path('./uploads').resolve()
ALLOWED_EXT = '.pdf'
PDF_HEADER = b'%PDF-'

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return jsonify({'error': 'file required'}), 400
    file = request.files['pdf']
    if not file or not file.filename:
        return jsonify({'error': 'file required'}), 400
    filename = secure_filename(file.filename)
    if not filename.lower().endswith(ALLOWED_EXT):
        return jsonify({'error': 'invalid file type'}), 400
    # check PDF magic bytes
    try:
        header = file.stream.read(len(PDF_HEADER))
        file.stream.seek(0)
    except Exception:
        return jsonify({'error': 'invalid file'}), 400
    if header != PDF_HEADER:
        return jsonify({'error': 'invalid pdf'}), 400
    UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    unique_name = f"{Path(filename).stem}_{uuid.uuid4().hex}{ALLOWED_EXT}"
    dest = UPLOAD_DIR / unique_name
    try:
        # ensure destination is inside upload directory
        if not str(dest.resolve()).startswith(str(UPLOAD_DIR) + os.sep):
            return jsonify({'error': 'invalid file path'}), 400
        file.save(str(dest))
    except Exception:
        return jsonify({'error': 'upload failed'}), 500
    return jsonify({'filename': unique_name}), 201
