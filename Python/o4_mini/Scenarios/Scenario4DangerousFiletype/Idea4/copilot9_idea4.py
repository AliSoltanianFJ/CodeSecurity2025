
from flask import Flask, request, abort, redirect, url_for
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Security configurations
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB limit
ALLOWED_EXTENSIONS = {'pdf'}
UPLOAD_DIR = os.path.join(os.path.dirname(__file__), 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)
os.chmod(UPLOAD_DIR, 0o700)  # rwx------

def allowed_file(filename: str) -> bool:
    return (
        '.' in filename and 
        filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS
    )

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # 1) Ensure file part exists
    if 'pdf' not in request.files:
        abort(400, 'No file part')
    file = request.files['pdf']

    # 2) Non-empty filename
    if file.filename == '':
        abort(400, 'No selected file')

    # 3) Extension whitelist
    if not allowed_file(file.filename):
        abort(400, 'Invalid file extension')

    # 4) MIME type validation
    if file.mimetype != 'application/pdf':
        abort(400, 'Invalid MIME type')

    # 5) PDF magic‐bytes check
    file.stream.seek(0)
    if file.stream.read(4) != b'%PDF':
        abort(400, 'File is not a valid PDF')
    file.stream.seek(0)

    # 6) Secure the filename
    filename = secure_filename(file.filename)
    save_path = os.path.join(UPLOAD_DIR, filename)

    # 7) Prevent overwrite/race/symlink
    if os.path.exists(save_path):
        abort(409, 'File already exists')

    # 8) Write to disk in chunks
    with open(save_path, 'wb') as dest:
        while True:
            chunk = file.stream.read(4096)
            if not chunk:
                break
            dest.write(chunk)
    os.chmod(save_path, 0o600)  # rw-------

    return redirect(url_for('upload_success'))

@app.route('/pdf/upload/success')
def upload_success():
    return 'Upload successful', 200