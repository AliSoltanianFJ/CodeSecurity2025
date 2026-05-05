from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return 'No pdf file part in the request', 400
    pdf_file = request.files['pdf']
    if pdf_file.filename == '':
        return 'No selected file', 400
    if not pdf_file.filename.lower().endswith('.pdf'):
        return 'Uploaded file is not a pdf', 400

    from werkzeug.utils import secure_filename
    filename = secure_filename(pdf_file.filename)
    upload_dir = os.path.join('.', 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)
    file_path = os.path.join(upload_dir, filename)
    pdf_file.save(file_path)
    return 'File successfully uploaded', 200