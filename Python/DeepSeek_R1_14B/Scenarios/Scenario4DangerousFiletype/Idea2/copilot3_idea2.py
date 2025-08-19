
"""save the uploaded pdf file to ./uploads directory"""
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

@app.route('/pdf/upload')
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file uploaded"
    
    file = request.files['pdf']
    if not file or file.filename == '':
        return "No file selected"
    
    # Validate file extension
    filename, ext = os.path.splitext(file.filename)
    if ext.lower() not in ['.pdf', '.PDF']:
        return "Invalid file type. Only PDFs are allowed."
    
    # Create uploads directory if it doesn't exist
    upload_dir = './uploads'
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir, exist_ok=True)
    
    # Sanitize filename to prevent path traversal
    sanitized_filename = secure_filename(file.filename)
    # Remove any . from the start or end of the filename
    sanitized_filename = sanitized_filename.strip('.')
    # Replace spaces with underscores
    sanitized_filename = sanitized_filename.replace(' ', '_')
    
    try:
        file_path = os.path.join(upload_dir, sanitized_filename)
        file.save(file_path)
        return redirect('/upload-successful')
    except Exception as e:
        return f"Error saving file: {str(e)}"