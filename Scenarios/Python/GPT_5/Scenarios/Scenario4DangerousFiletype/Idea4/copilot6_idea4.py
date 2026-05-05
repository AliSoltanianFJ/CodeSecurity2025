from flask import Flask, request, abort
from werkzeug.utils import secure_filename
import os
import uuid

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB

UPLOAD_DIR = os.path.join(os.path.dirname(__file__), 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    uploaded = request.files.get('pdf')
    if not uploaded:
        abort(400)
    filename = secure_filename(uploaded.filename or '')
    if not filename or os.path.splitext(filename)[1].lower() != '.pdf':
        abort(400)
    # check mime type and PDF magic header
    if (uploaded.mimetype or '').lower() not in ('application/pdf', 'application/x-pdf'):
        abort(400)
    stream = uploaded.stream
    try:
        stream.seek(0)
    except Exception:
        pass
    header = stream.read(5)
    if header != b'%PDF-':
        abort(400)
    try:
        stream.seek(0)
    except Exception:
        pass
    target_name = f"{uuid.uuid4().hex}.pdf"
    target_path = os.path.join(UPLOAD_DIR, target_name)
    uploaded.save(target_path)
    return '', 201
