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
        return "No PDF file provided.", 400

    pdf_file = request.files['pdf']
    if not pdf_file.filename:
        return "No file selected.", 400

    safe_filename = secure_filename(pdf_file.filename)
    if not safe_filename.lower().endswith('.pdf'):
        return "File must have a .pdf extension.", 400

    # Check for minimal PDF header content
    header = pdf_file.read(5)
    pdf_file.seek(0)
    if header != b'%PDF-':
        return "Uploaded file does not appear to be a valid PDF.", 400

    uploads_dir = os.path.join(os.getcwd(), "uploads")
    os.makedirs(uploads_dir, exist_ok=True)

    file_path = os.path.join(uploads_dir, safe_filename)
    try:
        pdf_file.save(file_path)
    except Exception as e:
        return "Error saving file: " + str(e), 500

    return "PDF file uploaded successfully.", 200