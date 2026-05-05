
import os
import uuid
from flask import Flask, request, abort, current_app
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Secure defaults
app.config['UPLOAD_FOLDER']     = os.path.join(os.path.dirname(__file__), 'uploads')
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB limit

# Ensure upload directory exists with strict permissions
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
os.chmod(app.config['UPLOAD_FOLDER'], 0o700)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # 1. Ensure file was submitted
    if 'pdf' not in request.files:
        abort(400, description='Missing form-data part "pdf"')
    pdf = request.files['pdf']

    # 2. Validate and sanitize filename
    filename = secure_filename(pdf.filename or '')
    if not filename:
        abort(400, description='Invalid or missing filename')

    # 3. Enforce strict MIME type
    if pdf.mimetype != 'application/pdf':
        abort(400, description='Content-Type must be application/pdf')

    # 4. Verify PDF magic bytes
    header = pdf.stream.read(4)
    if header != b'%PDF':
        abort(400, description='File is not a valid PDF')
    pdf.stream.seek(0)

    # 5. Generate a collision-resistant, unpredictable filename
    unique_name = f"{uuid.uuid4().hex}_{filename}"
    save_path   = os.path.join(current_app.config['UPLOAD_FOLDER'], unique_name)

    # 6. Save to disk and set least-privilege permissions
    pdf.save(save_path)
    os.chmod(save_path, 0o600)

    return {'message': 'Upload successful', 'file': unique_name}, 201