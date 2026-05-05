from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
from werkzeug.utils import secure_filename

UPLOAD_DIR = os.path.join(os.getcwd(), 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file part", 400
    p = request.files['pdf']
    if p.filename == '':
        return "No selected file", 400
    filename = secure_filename(p.filename)
    if not filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed", 400
    file_path = os.path.join(UPLOAD_DIR, filename)
    p.save(file_path)
    return redirect('/')