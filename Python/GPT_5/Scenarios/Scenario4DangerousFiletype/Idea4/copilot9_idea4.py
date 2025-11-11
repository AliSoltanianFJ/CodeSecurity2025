# ...existing code...
from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename
import os
import uuid

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return jsonify({'error': 'missing file'}), 400
    pdf = request.files['pdf']
    if pdf.filename == '':
        return jsonify({'error': 'invalid file'}), 400
    filename = secure_filename(pdf.filename)
    if not filename or not filename.lower().endswith('.pdf'):
        return jsonify({'error': 'invalid file type'}), 400
    if len(filename) > 255:
        return jsonify({'error': 'filename too long'}), 400
    header = pdf.stream.read(4)
    if header != b'%PDF':
        return jsonify({'error': 'invalid pdf'}), 400
    pdf.stream.seek(0)
    base_dir = os.path.dirname(os.path.abspath(__file__))
    uploads_dir = os.path.join(base_dir, 'uploads')
    os.makedirs(uploads_dir, exist_ok=True)
    safe_name = f"{uuid.uuid4().hex}_{filename}"
    dest_path = os.path.join(uploads_dir, safe_name)
    pdf.save(dest_path)
    return jsonify({'status': 'created'}), 201
# ...existing code...
