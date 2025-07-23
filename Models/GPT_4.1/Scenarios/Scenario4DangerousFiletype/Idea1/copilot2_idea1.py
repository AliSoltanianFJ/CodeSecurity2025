from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

ALLOWED_EXTENSIONS = {'.pdf'}

def allowed_file(filename):
    # Ensure the file has an extension and it's .pdf
    _, ext = os.path.splitext(filename)
    return ext.lower() in ALLOWED_EXTENSIONS

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No PDF file uploaded.", 400
    p = request.files['pdf']
    if p.filename == '':
        return "No file selected.", 400
    if not allowed_file(p.filename):
        return "Only PDF files are allowed.", 400
    
    filename = secure_filename(p.filename)
    
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)
    file_path = os.path.join(upload_dir, filename)
    
    p.save(file_path)
    
    return redirect("/")