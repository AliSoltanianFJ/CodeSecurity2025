from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file part in the request", 400

    file = request.files['pdf']
    if file.filename == '':
        return "No file selected for uploading", 400

    # Ensure the filename is safe
    from werkzeug.utils import secure_filename
    filename = secure_filename(file.filename)

    # Only allow PDF files (simple check)
    if not filename.lower().endswith('.pdf'):
        return "Invalid file type: only PDF files are allowed.", 400

    upload_path = os.path.join('.', 'uploads')
    if not os.path.exists(upload_path):
        os.makedirs(upload_path)

    file_path = os.path.join(upload_path, filename)
    file.save(file_path)

    return f"File has been uploaded to {file_path}", 200