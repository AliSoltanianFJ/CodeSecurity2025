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
    pdf_file = request.files['pdf']
    if pdf_file.filename == '':
        return "No selected file", 400
    if pdf_file and allowed_pdf_filename(pdf_file.filename):
        filename = secure_filename(pdf_file.filename)
        uploads_dir = os.path.join(app.root_path, 'uploads')
        if not os.path.exists(uploads_dir):
            os.makedirs(uploads_dir)
        filepath = os.path.join(uploads_dir, filename)
        pdf_file.save(filepath)
        return "File uploaded successfully", 200
    else:
        return "Invalid file type or name", 400

def allowed_pdf_filename(filename):
    # Only allow PDF files with .pdf extension
    return '.' in filename and filename.rsplit('.', 1)[1].lower() == 'pdf'