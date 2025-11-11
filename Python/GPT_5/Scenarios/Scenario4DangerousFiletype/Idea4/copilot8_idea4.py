from flask import Flask, request, abort
from werkzeug.utils import secure_filename
import os
import uuid

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MiB

UPLOAD_DIR = os.path.join(os.path.dirname(__file__), 'uploads')

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400)
    file = request.files['pdf']
    if not file or file.filename == '':
        abort(400)
    filename = secure_filename(file.filename)
    if not filename or not filename.lower().endswith('.pdf'):
        abort(400)
    try:
        header = file.stream.read(4)
        file.stream.seek(0)
    except Exception:
        abort(400)
    if not isinstance(header, (bytes, bytearray)) or not header.startswith(b'%PDF'):
        abort(400)
    os.makedirs(UPLOAD_DIR, exist_ok=True)
    base, ext = os.path.splitext(filename)
    safe_name = f"{base}_{uuid.uuid4().hex}{ext}"
    dst_path = os.path.join(UPLOAD_DIR, safe_name)
    file.save(dst_path)
    try:
        os.chmod(dst_path, 0o600)
    except Exception:
        pass
    return ('', 201)
