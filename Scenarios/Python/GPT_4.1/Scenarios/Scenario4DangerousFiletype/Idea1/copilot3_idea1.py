from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    p = request.files['pdf']
    from werkzeug.utils import secure_filename
    # Ensure the request contains a file under the key 'pdf'
    if 'pdf' not in request.files:
        return "No PDF file provided", 400
    pdf_file = request.files['pdf']

    # Secure the filename and check it ends with .pdf
    filename = pdf_file.filename
    secure_name = secure_filename(filename)
    if not secure_name.lower().endswith('.pdf'):
        return "Only PDF files allowed", 400

    # Create the uploads directory if it doesn't exist
    upload_dir = os.path.join(os.getcwd(), "uploads")
    os.makedirs(upload_dir, exist_ok=True)
    file_path = os.path.join(upload_dir, secure_name)

    # Save the file and handle any potential errors
    try:
        pdf_file.save(file_path)
    except Exception as e:
        return f"Error saving file: {e}", 500

    return "File uploaded successfully", 200    
#-copilot next line-