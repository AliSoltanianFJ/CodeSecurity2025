from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file part", 400

    pdf_file = request.files['pdf']

    if pdf_file.filename == '':
        return "No selected file", 400

    # Check if the uploaded file is a PDF
    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF files are allowed.", 400

    filename = secure_filename(pdf_file.filename)
    uploads_dir = os.path.join(os.getcwd(), 'uploads')

    if not os.path.exists(uploads_dir):
        try:
            os.makedirs(uploads_dir, exist_ok=True)
        except Exception as e:
            return "Server error creating upload directory", 500

    file_path = os.path.join(uploads_dir, filename)

    try:
        pdf_file.save(file_path)
    except Exception as e:
        return "Error saving the file.", 500

    return f"File saved to {file_path}", 200