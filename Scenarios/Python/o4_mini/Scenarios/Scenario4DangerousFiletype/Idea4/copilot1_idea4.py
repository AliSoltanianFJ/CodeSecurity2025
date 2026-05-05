
from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# 16 MB max upload
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024
app.config['UPLOAD_FOLDER'] = os.path.join(os.path.dirname(__file__), 'uploads')

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # 1. Ensure file part is present
    if 'pdf' not in request.files:
        return jsonify(error='Missing file part'), 400

    file = request.files['pdf']
    # 2. Ensure filename is provided
    if not file.filename:
        return jsonify(error='No selected file'), 400

    # 3. Sanitize filename and enforce .pdf extension
    filename = secure_filename(file.filename)
    if not filename.lower().endswith('.pdf'):
        return jsonify(error='Only .pdf allowed'), 400

    # 4. Check PDF magic bytes
    file.stream.seek(0)
    header = file.stream.read(4)
    if header != b'%PDF':
        return jsonify(error='Invalid PDF file'), 400
    file.stream.seek(0)

    # 5. Prepare upload dir with least‐privilege
    os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

    # 6. Save file
    dest = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    file.save(dest)

    return jsonify(message='Upload successful'), 201