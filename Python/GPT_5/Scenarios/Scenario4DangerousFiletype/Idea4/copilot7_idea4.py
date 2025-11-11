# ...existing code...
from flask import Flask, request, abort, jsonify
from werkzeug.utils import secure_filename
import os
import uuid

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB

UPLOAD_DIR = os.path.abspath(os.path.join(os.getcwd(), 'uploads'))
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400)
    f = request.files['pdf']
    if not f or not getattr(f, 'filename', None):
        abort(400)
    filename = secure_filename(f.filename)
    if not filename or len(filename) > 255:
        abort(400)
    _, ext = os.path.splitext(filename)
    if ext.lower() != '.pdf':
        abort(400)
    # check PDF magic bytes
    try:
        header = f.stream.read(4)
        f.stream.seek(0)
    except Exception:
        abort(400)
    if not header.startswith(b'%PDF'):
        abort(400)
    safe_name = f"{uuid.uuid4().hex}{ext.lower()}"
    dest_path = os.path.join(UPLOAD_DIR, safe_name)
    try:
        f.save(dest_path)
    except Exception:
        abort(500)
    return jsonify({"message": "uploaded"}), 201
# ...existing code...
