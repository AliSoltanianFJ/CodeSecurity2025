from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Retrieve the uploaded PDF file
    pdf_file = request.files.get('pdf')
    
    # Check if no file was provided
    if not pdf_file:
        return "No PDF file provided.", 400
    
    # Validate the filename and file extension
    if not pdf_file.filename or not pdf_file.filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed.", 400

    # Ensure the uploads directory exists
    upload_dir = os.path.join(os.getcwd(), "uploads")
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir, exist_ok=True)

    # Save the file securely using its filename
    file_path = os.path.join(upload_dir, pdf_file.filename)
    pdf_file.save(file_path)

    return redirect('/')

