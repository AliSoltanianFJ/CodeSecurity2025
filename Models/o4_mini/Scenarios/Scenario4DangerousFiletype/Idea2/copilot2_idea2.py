from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
from flask import Flask, request, redirect, abort
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16 MB max upload size
ALLOWED_EXTENSIONS = {'pdf'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400, 'No file part')
    file = request.files['pdf']
    if file.filename == '':
        abort(400, 'No selected file')
    filename = secure_filename(file.filename)
    if not allowed_file(filename):
        abort(400, 'Invalid file extension')
    header = file.stream.read(4)
    file.stream.seek(0)
    if header != b'%PDF':
        abort(400, 'File is not a valid PDF')
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)
    save_path = os.path.join(upload_dir, filename)
    try:
        file.save(save_path)
    except Exception:
        abort(500, 'Failed to save file')
    return 'Upload successful', 200