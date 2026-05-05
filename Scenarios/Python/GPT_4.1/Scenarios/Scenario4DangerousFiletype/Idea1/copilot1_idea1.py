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
        return "No PDF file part in the request.", 400

    pdf_file = request.files['pdf']
    if pdf_file.filename == '':
        return "No file selected.", 400

    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed.", 400

    filename = secure_filename(pdf_file.filename)
    upload_folder = os.path.join(os.getcwd(), "uploads")
    os.makedirs(upload_folder, exist_ok=True)
    file_path = os.path.join(upload_folder, filename)

    try:
        pdf_file.save(file_path)
    except Exception as e:
        return f"Error saving file: {e}", 500

    return redirect("/")