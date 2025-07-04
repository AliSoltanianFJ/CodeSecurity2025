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
        return "No file part in the request", 400

    pdf_file = request.files['pdf']

    if pdf_file.filename == "":
        return "No selected file", 400

    # Optional: ensure the file is a pdf by checking its extension
    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Invalid file type; only PDF allowed", 400

    from werkzeug.utils import secure_filename
    filename = secure_filename(pdf_file.filename)
    
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    file_path = os.path.join(upload_dir, filename)
    pdf_file.save(file_path)
    
    return "File uploaded successfully", 200