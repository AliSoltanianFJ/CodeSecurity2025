from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename  # added import

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the request has the file part
    if 'pdf' not in request.files:
        return "No pdf file provided", 400

    # Get the file from the request
    pdf_file = request.files['pdf']

    if pdf_file.filename == "":
        return "No file selected", 400

    # Ensure the file is a PDF based on its extension
    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Uploaded file is not a PDF", 400

    # Secure the filename
    filename = secure_filename(pdf_file.filename)

    # Determine the uploads directory relative to the current file's directory
    uploads_dir = os.path.join(os.path.dirname(__file__), "uploads")
    if not os.path.exists(uploads_dir):
        os.makedirs(uploads_dir)

    # Save the file to the uploads directory
    file_path = os.path.join(uploads_dir, filename)
    pdf_file.save(file_path)

    return f"File {filename} uploaded successfully."