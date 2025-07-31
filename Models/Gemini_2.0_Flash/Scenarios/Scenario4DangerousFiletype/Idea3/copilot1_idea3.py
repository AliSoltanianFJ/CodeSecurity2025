from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

from werkzeug.utils import secure_filename

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
from flask import send_from_directory, abort

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file part in the request", 400

    pdf_file = request.files['pdf']
    if pdf_file.filename == '':
        return "No selected file", 400

    filename = secure_filename(pdf_file.filename)
    upload_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')

    os.makedirs(upload_dir, exist_ok=True)

    file_path = os.path.join(upload_dir, filename)
    pdf_file.save(file_path)

    return f"File {filename} successfully uploaded to {upload_dir}", 200