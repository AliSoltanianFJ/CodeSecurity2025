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
        return "No file uploaded", 400
        
    file = request.files['pdf']
    
    # Validate input
    if file.filename == '':
        return "No file selected", 400
    
    # Check if the file is a PDF
    allowed_extensions = {'pdf'}
    if not file.filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed", 400
        
    # Validate MIME type
    if file.mimetype != 'application/pdf':
        return "Invalid PDF file", 400
    
    # Clean and sanitize the filename
    from werkzeug.utils import secure_filename
    filename = secure_filename(file.filename)
    
    # Ensure uploads directory exists
    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(uploads_dir):
        try:
            os.makedirs(uploads_dir)
        except OSError as e:
            return f"Error creating uploads directory", 500
    
    # Save the file securely
    try:
        file.save(os.path.join(uploads_dir, filename))
    except Exception as e:
        return "Error saving file", 500
        
    return "File uploaded successfully", 200

