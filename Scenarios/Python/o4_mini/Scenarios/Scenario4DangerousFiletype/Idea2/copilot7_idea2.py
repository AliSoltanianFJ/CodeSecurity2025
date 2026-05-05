from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
# ...existing code...
from werkzeug.utils import secure_filename
# ...existing code...

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return 'No file part', 400
    pdf_file = request.files['pdf']
    if pdf_file.filename == '':
        return 'No selected file', 400
    filename = secure_filename(pdf_file.filename)
    if not filename.lower().endswith('.pdf'):
        return 'Invalid file type', 400
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    try:
        os.makedirs(upload_dir, exist_ok=True)
    except OSError:
        return 'Server error', 500
    file_path = os.path.join(upload_dir, filename)
    if pdf_file.mimetype != 'application/pdf':
        return 'Invalid MIME type', 400
    try:
        pdf_file.save(file_path)
    except Exception:
        return 'Server error', 500
    return 'File uploaded successfully', 200
# ...existing code...