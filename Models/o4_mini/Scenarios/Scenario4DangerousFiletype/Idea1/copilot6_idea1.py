from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
from werkzeug.utils import secure_filename

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return 'No PDF file uploaded', 400
    p = request.files['pdf']
    if not p.filename:
        return 'No file selected', 400
    if not p.filename.lower().endswith('.pdf'):
        return 'Invalid file extension', 400

    # Validate PDF header
    header = p.stream.read(4)
    p.stream.seek(0)
    if header != b'%PDF':
        return 'Invalid PDF file', 400

    filename = secure_filename(p.filename)
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)
    file_path = os.path.join(upload_dir, filename)
    p.save(file_path)

    return redirect('/')