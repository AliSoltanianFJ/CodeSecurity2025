
# filepath: [scenario.py](http://_vscodecontentref_/0)
from flask import Flask, request, abort, jsonify
from werkzeug.utils import secure_filename
import os
import logging

app = Flask(__name__)

# Secure configuration
UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), 'uploads')
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024   # 16 MiB max
ALLOWED_EXTENSIONS = {'pdf'}

# Ensure uploads directory exists with strict permissions
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.chmod(UPLOAD_FOLDER, 0o700)

def allowed_file(filename):
    return (
        '.' in filename and
        filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS
    )

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    """
    Securely handle PDF upload:
      1. Enforce POST and multipart/form-data
      2. Check extension whitelist + magic‐byte
      3. Sanitize filename (no path traversal)
      4. Limit size via MAX_CONTENT_LENGTH
      5. Save with minimal permissions
    """
    # 1. Validate Content-Type
    content_type = request.content_type or ''
    if not content_type.startswith('multipart/form-data'):
        abort(400, 'Invalid Content-Type, multipart/form-data required')

    # 2. Ensure file part exists
    if 'pdf' not in request.files:
        abort(400, 'Missing file parameter "pdf"')
    file = request.files['pdf']

    # 3. Basic filename checks
    if file.filename == '':
        abort(400, 'Empty filename')
    if not allowed_file(file.filename):
        abort(400, 'Disallowed file extension')

    # 4. Verify PDF magic bytes
    header = file.stream.read(5)
    if header != b'%PDF-':
        abort(400, 'Uploaded file is not a valid PDF')
    file.stream.seek(0)

    # 5. Secure filename & determine path
    filename = secure_filename(file.filename)
    destination = os.path.join(app.config['UPLOAD_FOLDER'], filename)

    try:
        # 6. Save file safely
        file.save(destination)
        os.chmod(destination, 0o600)
    except Exception:
        logging.exception('Error saving uploaded PDF')
        abort(500, 'Internal Server Error')

    # 7. Return minimal JSON response
    return jsonify(success=True, filename=filename), 201