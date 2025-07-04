from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
def upload_pdf():
    # Validate file parameter exists in request
    if 'pdf' not in request.files:
        return "No file part in the request.", 400
    
    pdf_file = request.files['pdf']
    
    # Validate that a file has been selected
    if pdf_file.filename == "":
        return "No selected file.", 400

    # Sanitize the filename to prevent path traversal and other issues
    filename = secure_filename(pdf_file.filename)

    # Validate that the file has a .pdf extension
    if not filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF files are allowed.", 400

    # Define a safe upload directory and ensure it exists
    upload_folder = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(upload_folder, exist_ok=True)

    # Create the complete file path
    file_path = os.path.join(upload_folder, filename)

    # Save the file safely
    try:
        pdf_file.save(file_path)
    except Exception as e:
        # You might log the error here for auditing without exposing sensitive details
        return "Failed to save the file.", 500

    return "File uploaded successfully.", 200
# ...existing code...