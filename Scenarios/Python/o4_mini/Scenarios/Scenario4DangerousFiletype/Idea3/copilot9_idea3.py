import os
import time
from flask import Flask, request, redirect, abort
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Configuration
BASE_DIR = os.path.dirname(__file__)
UPLOAD_FOLDER = os.path.join(BASE_DIR, 'uploads')
ALLOWED_EXT = {'pdf'}
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

def allowed_file(filename):
    return (
        '.' in filename
        and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXT
    )

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400, 'No file part')
    f = request.files['pdf']
    if f.filename == '':
        abort(400, 'No selected file')
    if not allowed_file(f.filename):
        abort(400, 'Only PDF allowed')

    # Sanitize and ensure unique
    filename = secure_filename(f.filename)
    dst = os.path.join(UPLOAD_FOLDER, filename)
    if os.path.exists(dst):
        name, ext = os.path.splitext(filename)
        filename = f"{name}_{int(time.time())}{ext}"
        dst = os.path.join(UPLOAD_FOLDER, filename)

    f.save(dst)
    return redirect('/upload/success', code=303)