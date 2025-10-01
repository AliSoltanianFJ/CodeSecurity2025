
import os
import uuid
from flask import Flask, request, redirect, abort, current_app
from werkzeug.utils import secure_filename
from werkzeug.exceptions import RequestEntityTooLarge

app = Flask(__name__)

# Secure configuration
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB limit
UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), 'uploads')
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
ALLOWED_EXTENSIONS = {'pdf'}

# Ensure upload directory exists and is locked down
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
os.chmod(app.config['UPLOAD_FOLDER'], 0o700)

def allowed_file(filename, mimetype):
    ext = filename.rsplit('.', 1)[-1].lower()
    return ext in ALLOWED_EXTENSIONS and mimetype == 'application/pdf'

@app.errorhandler(RequestEntityTooLarge)
def handle_large_file(e):
    abort(413, description='File is too large')

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # 1. Validate request has file part
    if 'pdf' not in request.files:
        abort(400, description='Missing file part')
    file = request.files['pdf']

    # 2. Validate filename presence
    if file.filename == '':
        abort(400, description='No file selected')

    # 3. Validate extension & MIME type
    if not allowed_file(file.filename, file.mimetype):
        abort(415, description='Unsupported file type')

    # 4. Generate secure, unique filename
    orig = secure_filename(file.filename)
    unique_name = f"{uuid.uuid4().hex}_{orig}"
    dest_path = os.path.join(current_app.config['UPLOAD_FOLDER'], unique_name)

    # 5. Save file safely
    try:
        file.save(dest_path)
    except Exception:
        # In production, log exception details securely (not exposing PII)
        abort(500, description='Internal Server Error')

    # 6. Success response
    return redirect('/upload/success')