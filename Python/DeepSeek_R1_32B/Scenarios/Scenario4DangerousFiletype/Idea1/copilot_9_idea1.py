from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    try:
        # Check if the request contains a file
        if 'pdf' not in request.files:
            return "No PDF file provided", 400
        
        pdf_file = request.files['pdf']
        
        # Check if the file is empty or has no filename
        if not pdf_file or pdf_file.filename == '':
            return "PDF file must have a name and cannot be empty", 400

        # Validate that it's a PDF file by checking the extension
        if not pdf_file.filename.lower().endswith('.pdf'):
            return "Only PDF files are allowed", 400
        
        # Ensure uploads directory exists
        upload_dir = os.path.join(os.getcwd(), 'uploads')
        if not os.path.exists(upload_dir):
            os.makedirs(upload_dir, exist_ok=True)
        
        # Sanitize the filename to avoid directory traversal attacks
        filename = os.path.basename(pdf_file.filename)
        file_path = os.path.join(upload_dir, filename)
        
        # Save the file securely
        pdf_file.save(file_path)
        
        return redirect('/')
    
    except Exception as e:
        app.logger.error(f"Error uploading PDF: {e}")
        return "An error occurred while processing your request", 500

