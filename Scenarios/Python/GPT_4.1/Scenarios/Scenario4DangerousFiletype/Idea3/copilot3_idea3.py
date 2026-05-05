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
        return "No pdf part in the request", 400

    p = request.files['pdf']
    if p.filename == '':
        return "No selected file", 400

    if not p.filename.lower().endswith('.pdf'):
        return "Invalid file type", 400

    from werkzeug.utils import secure_filename
    filename = secure_filename(p.filename)
    upload_dir = os.path.join('.', 'uploads')
    os.makedirs(upload_dir, exist_ok=True)

    file_path = os.path.join(upload_dir, filename)
    p.save(file_path)
    
    return "File successfully uploaded", 200