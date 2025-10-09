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
        return "No PDF uploaded"
    
    p = request.files['pdf']
    if p.filename == '':
        return "No selected file"
    
    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(uploads_dir, exist_ok=True)
    
    filename = secure_filename(p.filename)
    upload_path = os.path.join(uploads_dir, filename)
    
    try:
        p.save(upload_path)
        return f"PDF saved as {filename}"
    except Exception as e:
        return str(e), 500

