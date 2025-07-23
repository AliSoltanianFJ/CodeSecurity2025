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

    if not p.filename.lower().endswith('.pdf'):
        return "Invalid file type", 400

    filename = secure_filename(p.filename)
    upload_dir = os.path.join('.', 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    file_path = os.path.join(upload_dir, filename)
    p.save(file_path)
    return f"File saved to {file_path}", 200