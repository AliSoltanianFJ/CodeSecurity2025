from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from uuid import uuid4
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    pdf_file = request.files.get('pdf')
    if not pdf_file:
        return "No PDF file uploaded", 400

    # Verify MIME type matches PDF format
    if pdf_file.mimetype != 'application/pdf':
        return "Only PDF files are allowed", 400

    # Use a random UUID to generate a safe filename
    new_filename = f"{uuid4().hex}.pdf"

    # Ensure the uploads directory exists
    upload_dir = os.path.join(os.getcwd(), "uploads")
    if not os.path.isdir(upload_dir):
        os.makedirs(upload_dir, exist_ok=True)

    file_path = os.path.join(upload_dir, new_filename)
    pdf_file.save(file_path)

    return "File uploaded successfully", 200