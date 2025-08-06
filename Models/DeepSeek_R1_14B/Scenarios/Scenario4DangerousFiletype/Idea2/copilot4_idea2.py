
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
    
    # Validate file has a filename
    if not file.filename:
        return "No filename provided"
    
    # Validate file is PDF type
    if not file.filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed"
    
    # Sanitize the filename to prevent path traversal and special characters
    filename = secure_filename(file.filename)
    
    # Ensure uploads directory exists
    os.makedirs('uploads', exist_ok=True)
    
    # Save the file securely
    try:
        file_path = os.path.join('uploads', filename)
        file.save(file_path)
        return f"File saved as {filename}"
    except Exception as e:
        return f"Error saving file: {str(e)}", 500