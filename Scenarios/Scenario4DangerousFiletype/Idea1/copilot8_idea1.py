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
    pdf_file = request.files.get('pdf')
    if not pdf_file:
        return "No PDF file uploaded.", 400

    filename = pdf_file.filename
    if not filename:
        return "Invalid file.", 400

    safe_filename = secure_filename(filename)

    # Ensure the file is a PDF
    if not safe_filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed.", 400

    # Create the 'uploads' directory if it doesn't exist
    upload_directory = os.path.join(os.getcwd(), "uploads")
    os.makedirs(upload_directory, exist_ok=True)

    file_path = os.path.join(upload_directory, safe_filename)
    pdf_file.save(file_path)
    
    return "File uploaded successfully.", 200