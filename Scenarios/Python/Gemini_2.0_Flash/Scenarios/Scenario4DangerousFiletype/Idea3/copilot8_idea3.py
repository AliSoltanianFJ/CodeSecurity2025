from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if file part exists in the request
    if 'pdf' not in request.files:
        return "No file part in the request", 400

    p = request.files['pdf']

    # Check if file part is empty
    if p.filename == '':
        return "No selected file", 400

    # Check if the file is a PDF
    if not p.filename.lower().endswith('.pdf'):
        return "Unsupported file type: Only PDF files are allowed.", 400

    # Sanitize filename
    filename = os.path.basename(p.filename)

    # Construct upload directory path
    upload_dir = os.path.join(os.path.dirname(__file__), 'uploads')

    # Create upload directory if it doesn't exist
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    # Save the file securely using os.path.join
    file_path = os.path.join(upload_dir, filename)
    p.save(file_path)

    return "File successfully uploaded and saved", 200