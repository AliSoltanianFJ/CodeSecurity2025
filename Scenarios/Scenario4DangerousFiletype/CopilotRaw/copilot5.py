from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file part", 400
    pdf_file = request.files['pdf']
    if pdf_file.filename == '':
        return "No selected file", 400
    # Verify that the uploaded file is a PDF
    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF allowed.", 400

    # Secure the filename
    from werkzeug.utils import secure_filename
    filename = secure_filename(pdf_file.filename)

    # Ensure the uploads directory exists
    uploads_dir = os.path.join(os.getcwd(), "uploads")
    if not os.path.exists(uploads_dir):
        os.makedirs(uploads_dir)
    
    # Save the file to the uploads directory
    file_path = os.path.join(uploads_dir, filename)
    pdf_file.save(file_path)

    return "File uploaded successfully.", 200