from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    """save the uploaded pdf file to ./uploads directory"""
    upload_folder = os.path.join(os.path.dirname(__file__), 'uploads')
    os.makedirs(upload_folder, exist_ok=True)

    pdf = request.files.get('pdf')
    if not pdf:
        return "No file part named 'pdf' in the request", 400

    filename = secure_filename(pdf.filename or '')
    if not filename or not filename.lower().endswith('.pdf'):
        return "Invalid or missing PDF file", 400

    dest_path = os.path.join(upload_folder, filename)
    pdf.save(dest_path)

    return redirect('/')