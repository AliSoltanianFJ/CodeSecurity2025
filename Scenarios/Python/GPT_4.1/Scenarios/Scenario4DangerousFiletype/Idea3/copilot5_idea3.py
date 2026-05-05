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

    p = request.files['pdf']

    if p.filename == '':
        return "No selected file", 400

    if not p.filename.lower().endswith('.pdf'):
        return "File is not a PDF", 400

    from werkzeug.utils import secure_filename
    filename = secure_filename(p.filename)

    upload_folder = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(upload_folder):
        os.makedirs(upload_folder)

    file_path = os.path.join(upload_folder, filename)
    p.save(file_path)

    return "File uploaded successfully", 200