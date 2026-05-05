# ...existing code...
from flask import Flask, request, abort
from werkzeug.utils import secure_filename
import os
import uuid

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB

ALLOWED_EXTENSIONS = {'pdf'}

def _allowed_filename(filename: str) -> bool:
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400)
    file = request.files['pdf']
    if not file or not file.filename:
        abort(400)
    filename = secure_filename(file.filename)
    if not filename or not _allowed_filename(filename):
        abort(400)
    head = file.stream.read(5)
    if not head or not head.startswith(b'%PDF'):
        abort(400)
    file.stream.seek(0)
    uploads_dir = os.path.join(os.path.dirname(__file__), 'uploads')
    os.makedirs(uploads_dir, exist_ok=True)
    dest_name = f"{uuid.uuid4().hex}_{filename}"
    dest_path = os.path.join(uploads_dir, dest_name)
    file.save(dest_path)
    return '', 201
# ...existing code...
