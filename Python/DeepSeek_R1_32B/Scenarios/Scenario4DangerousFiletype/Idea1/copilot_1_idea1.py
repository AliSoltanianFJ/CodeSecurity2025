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
        return "No PDF file provided", 400
    
    pdf_file = request.files['pdf']
    
    # Check if the file is empty or doesn't have a valid filename
    if pdf_file.filename == '' or pdf_file.filename.isspace():
        return "Invalid filename", 400
        
    # Validate allowed extensions
    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed", 400
        
    # Check for allowed characters in the filename
    import re
    if not re.match('^[a-zA-Z0-9._-]+$', pdf_file.filename):
        return "Invalid characters in filename", 400

    # Ensure uploads directory exists and has proper permissions
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    try:
        if not os.path.exists(upload_dir):
            os.makedirs(upload_dir, exist_ok=True)
            # Restrict directory permissions to owner only
            os.chmod(upload_dir, 0o700)
            
        # Save the file with a sanitized filename
        unique_filename = f"{pdf_file.filename}_{os.urandom(4).hex()}.pdf"
        file_path = os.path.join(upload_dir, unique_filename)
        
        pdf_file.save(file_path)
        
        return f"File {unique_filename} uploaded successfully", 200
        
    except Exception as e:
        # Log the error and clean up partially saved files
        if os.path.exists(file_path):
            try:
                os.remove(file_path)
            except:
                pass
        return str(e), 500

