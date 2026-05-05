
from flask import Flask, request, abort, jsonify
import os, uuid
from werkzeug.utils import secure_filename

# ---- Secure Configuration & Defense‐in‐Depth ----
UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), 'uploads')
ALLOWED_EXTENSIONS = {'pdf'}
MAX_CONTENT_LENGTH = 10 * 1024 * 1024   # 10 MB limit

app = Flask(__name__)
app.config.update({
    'UPLOAD_FOLDER': UPLOAD_FOLDER,
    'MAX_CONTENT_LENGTH': MAX_CONTENT_LENGTH,
    'DEBUG': False,
    'SESSION_COOKIE_SECURE': True,
    'SESSION_COOKIE_HTTPONLY': True,
    'SESSION_COOKIE_SAMESITE': 'Lax',
})

# Ensure upload directory exists with strict permissions
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
os.chmod(app.config['UPLOAD_FOLDER'], 0o700)

def allowed_file(filename: str) -> bool:
    return (
        '.' in filename and 
        filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS
    )

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # 1. Validate request
    if 'pdf' not in request.files:
        abort(400, description="Missing 'pdf' file part")
    file = request.files['pdf']
    if file.filename == '':
        abort(400, description="Empty filename")
    if not allowed_file(file.filename):
        abort(400, description="Invalid file extension")

    # 2. Content sniffing check: PDF magic bytes
    file.stream.seek(0)
    header = file.stream.read(5)
    file.stream.seek(0)
    if header != b'%PDF-':
        abort(400, description="File is not a valid PDF")

    # 3. Secure filename + unique token
    original = secure_filename(file.filename)
    unique_name = f"{uuid.uuid4().hex}_{original}"
    dest_path = os.path.join(app.config['UPLOAD_FOLDER'], unique_name)

    # 4. Ensure no path‐traversal
    abs_dest = os.path.abspath(dest_path)
    if not abs_dest.startswith(os.path.abspath(app.config['UPLOAD_FOLDER']) + os.sep):
        abort(400, description="Invalid file path")

    # 5. Save and harden file
    file.save(abs_dest)
    os.chmod(abs_dest, 0o600)

    # 6. Defense‐in‐depth: Avoid executing or loading PDF
    return jsonify({"message": "Upload successful", "filename": unique_name}), 201