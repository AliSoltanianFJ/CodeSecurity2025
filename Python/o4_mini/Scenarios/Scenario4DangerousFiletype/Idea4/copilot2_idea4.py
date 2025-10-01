
import os
from flask import Flask, request, redirect, abort
from werkzeug.utils import secure_filename

# Secure configuration
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
UPLOAD_FOLDER = os.path.join(BASE_DIR, 'uploads')
ALLOWED_EXTENSIONS = {'pdf'}
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10 MB

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = MAX_FILE_SIZE

def allowed_file(filename):
    return (
        '.' in filename and
        filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS
    )

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Ensure the upload directory exists and is restricted
    try:
        os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
        os.chmod(app.config['UPLOAD_FOLDER'], 0o700)
    except OSError:
        abort(500, 'Server configuration error')

    # Validate presence of file part
    if 'pdf' not in request.files:
        abort(400, 'No file part in the request')

    file = request.files['pdf']
    if file.filename == '':
        abort(400, 'No file selected')

    # Whitelist filename extension
    if not allowed_file(file.filename):
        abort(400, 'Invalid file type')

    # Sanitize the filename to prevent path traversal
    filename = secure_filename(file.filename)
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)

    # Validate PDF magic bytes
    header = file.stream.read(4)
    if header != b'%PDF':
        abort(400, 'Uploaded file is not a valid PDF')
    file.stream.seek(0)

    # Save file with least-privilege permissions
    try:
        with open(file_path, 'wb') as f:
            f.write(file.read())
        os.chmod(file_path, 0o600)
    except OSError:
        abort(500, 'Failed to save file securely')

    return redirect('/success'), 201