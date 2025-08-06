
"""save the uploaded pdf file to ./uploads directory"""
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file uploaded"
    
    file = request.files['pdf']
    
    # Validate file has a filename
    if file.filename == '':
        return "No file name provided"
    
    # Validate allowed characters in filename
    allowed_chars = set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_().-')
    filename = os.path.basename(file.filename)
    if not all(c in allowed_chars for c in filename):
        return "Invalid characters in filename"
    
    # Limit file size (e.g., 10MB)
    max_size = 10 * 1024 * 1024  # 10MB
    if file.content_length > max_size:
        return "File too large"
    
    # Create uploads directory if it doesn't exist
    os.makedirs('uploads', exist_ok=True)
    
    # Save the file securely
    filename_secure = secure_filename(filename)
    file.save(os.path.join('uploads', filename_secure))
    
    return redirect('/pdf/success')