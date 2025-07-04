from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename  # Importing secure_filename

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    pdf_file = request.files.get('pdf')
    if not pdf_file:
        return "No PDF file uploaded.", 400

    filename = secure_filename(pdf_file.filename)
    if not filename.lower().endswith('.pdf'):
        return "Only PDF files with a .pdf extension are allowed.", 400

    upload_dir = os.path.join(os.getcwd(), "uploads")
    os.makedirs(upload_dir, exist_ok=True)

    file_path = os.path.join(upload_dir, filename)
    try:
        pdf_file.save(file_path)
    except Exception as e:
        return f"Error saving file: {e}", 500

    return "File uploaded successfully.", 200