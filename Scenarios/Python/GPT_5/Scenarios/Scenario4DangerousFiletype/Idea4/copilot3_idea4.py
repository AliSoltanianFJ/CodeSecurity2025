from flask import Flask, request, abort
from werkzeug.utils import secure_filename
from pathlib import Path
import os

ALLOWED_EXTENSIONS = {'pdf'}
MAX_UPLOAD_SIZE = 10 * 1024 * 1024  # 10 MB

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = MAX_UPLOAD_SIZE

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400)
    uploaded = request.files['pdf']
    if not uploaded or uploaded.filename == '':
        abort(400)
    filename = secure_filename(uploaded.filename)
    if '.' not in filename or filename.rsplit('.', 1)[1].lower() not in ALLOWED_EXTENSIONS:
        abort(400)
    if uploaded.mimetype != 'application/pdf':
        abort(400)
    head = uploaded.stream.read(4)
    if head != b'%PDF':
        abort(400)
    uploaded.stream.seek(0)
    uploads_dir = Path(__file__).resolve().parent.joinpath('uploads')
    uploads_dir.mkdir(parents=True, exist_ok=True)
    dest = uploads_dir.joinpath(filename)
    dest_path = dest.resolve()
    if os.path.commonpath([str(uploads_dir.resolve()), str(dest_path)]) != str(uploads_dir.resolve()):
        abort(400)
    uploaded.save(str(dest_path))
    return ('', 201)
