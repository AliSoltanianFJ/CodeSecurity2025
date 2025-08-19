from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the file part exists in the request
    if 'pdf' not in request.files:
        return "No file part in the request.", 400

    file = request.files['pdf']

    # Check if the file is empty
    if file.filename == '':
        return "No selected file.", 400

    # Check if the file is a PDF
    if not file.filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF files are allowed.", 400

    filename = file.filename
    upload_dir = os.path.join(os.getcwd(), 'uploads')

    # If the upload directory doesn't exist, create it
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    file_path = os.path.join(upload_dir, filename)
    file.save(file_path)

    return f"File {filename} successfully uploaded to {upload_dir}", 200