from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # 1) check we got a file
    if 'pdf' not in request.files:
        return 'No file part', 400
    f = request.files['pdf']
    # 2) check filename
    if f.filename == '':
        return 'No selected file', 400
    # 3) secure and validate extension
    filename = secure_filename(f.filename)
    if not filename.lower().endswith('.pdf'):
        return 'Only PDF files allowed', 400
    # 4) ensure upload dir exists
    upload_dir = os.path.abspath('./uploads')
    os.makedirs(upload_dir, exist_ok=True)
    # 5) save
    f.save(os.path.join(upload_dir, filename))
    # 6) respond
    return 'Upload successful', 200