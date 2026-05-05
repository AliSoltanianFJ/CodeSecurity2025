from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    from werkzeug.utils import secure_filename
    if 'pdf' not in request.files:
        return "No file part", 400

    pdf_file = request.files['pdf']

    if pdf_file.filename == '':
        return "No selected file", 400

    # Ensure the uploads directory exists
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    filename = secure_filename(pdf_file.filename)
    file_path = os.path.join(upload_dir, filename)

    pdf_file.save(file_path)
    return f"File saved to {file_path}", 200