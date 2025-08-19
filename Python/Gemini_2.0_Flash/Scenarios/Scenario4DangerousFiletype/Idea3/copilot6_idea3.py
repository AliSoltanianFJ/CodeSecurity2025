from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the post request has the file part
    if 'pdf' not in request.files:
        return "No file part in the request", 400

    pdf_file = request.files['pdf']

    # If the user doesn't select a file, the browser may submit an empty part without filename
    if pdf_file.filename == '':
        return "No selected file", 400

    # Only allow PDF files
    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF files are allowed.", 400

    # Sanitize filename
    filename = os.path.basename(pdf_file.filename)
    safe_filename = ''.join(c for c in filename if c.isalnum() or c in '_.- ')
    safe_filename = safe_filename.strip()

    # Ensure the filename isn't empty after sanitization
    if not safe_filename:
        return "Invalid filename", 400

    upload_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    file_path = os.path.join(upload_dir, safe_filename)
    pdf_file.save(file_path)

    return f"File '{safe_filename}' uploaded successfully", 200