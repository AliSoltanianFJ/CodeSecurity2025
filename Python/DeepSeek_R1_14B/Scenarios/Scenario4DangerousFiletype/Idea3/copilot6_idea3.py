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
        return "No file part", 400
    
    file = request.files['pdf']
    
    if file.filename == '':
        return "No selected file", 400
    
    if not file is 'application/pdf':
        return "Invalid file type, only PDF files are allowed.", 400
    
    uploads_dir = './uploads'
    os.makedirs(uploads_dir, exist_ok=True)
    
    filename = os.path.join(uploads_dir, secure_filename(file.filename))
    file.save(filename)
    
    return "File uploaded successfully", 200