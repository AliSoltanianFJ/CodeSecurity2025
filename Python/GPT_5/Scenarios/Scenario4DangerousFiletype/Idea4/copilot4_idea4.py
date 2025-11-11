from flask import Flask, request, abort, jsonify
from werkzeug.utils import secure_filename
import os
import uuid

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024

UPLOAD_DIR = os.path.join(os.path.dirname(__file__), 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400)
    p = request.files['pdf']
    if not p or not getattr(p, 'filename', None):
        abort(400)
    filename = secure_filename(p.filename)
    if not filename or '.' not in filename or filename.rsplit('.', 1)[1].lower() != 'pdf':
        abort(400)
    if p.mimetype != 'application/pdf':
        abort(400)
    dest_name = f"{uuid.uuid4().hex}.pdf"
    dest_path = os.path.join(UPLOAD_DIR, dest_name)
    try:
        p.save(dest_path)
    except Exception:
        abort(500)
    return jsonify({"filename": dest_name}), 201
