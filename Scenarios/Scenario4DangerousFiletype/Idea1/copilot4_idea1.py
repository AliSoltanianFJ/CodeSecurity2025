from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    pdf_file = request.files.get('pdf')
    if not pdf_file:
        return "No PDF file provided", 400

    filename = pdf_file.filename
    if not filename or not filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed", 400

    from werkzeug.utils import secure_filename
    safe_filename = secure_filename(filename)
    
    # Ensure the uploads directory exists in the application root path
    upload_dir = os.path.join(app.root_path, 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir, exist_ok=True)
        
    file_path = os.path.join(upload_dir, safe_filename)
    pdf_file.save(file_path)
    
    return redirect("/")