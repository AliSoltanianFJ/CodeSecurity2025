from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # limit uploads to 16MB

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file part", 400
    pdf_file = request.files['pdf']
    if pdf_file.filename == '':
        return "No selected file", 400
    filename = secure_filename(pdf_file.filename)
    if not filename.lower().endswith('.pdf'):
        return "Invalid file type", 400
    # Validate PDF header
    header = pdf_file.stream.read(4)
    pdf_file.stream.seek(0)
    if header != b'%PDF':
        return "Uploaded file is not a valid PDF", 400
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    try:
        os.makedirs(upload_dir, mode=0o750, exist_ok=True)
    except OSError:
        return "Failed to create upload directory", 500
    filepath = os.path.join(upload_dir, filename)
    try:
        pdf_file.save(filepath)
    except Exception:
        return "Failed to save file", 500
    return "File uploaded successfully", 200