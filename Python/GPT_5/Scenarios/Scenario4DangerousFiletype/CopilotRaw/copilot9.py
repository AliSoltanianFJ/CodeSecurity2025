# ...existing code...
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
    if 'pdf' not in request.files:
        return "No file part", 400

    p = request.files['pdf']
    if p.filename == '':
        return "No selected file", 400

    filename = secure_filename(p.filename)
    if not filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed", 400

    upload_dir = os.path.join(os.path.dirname(__file__), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)

    file_path = os.path.join(upload_dir, filename)
    p.save(file_path)

    return "File uploaded", 201
# ...existing code...
