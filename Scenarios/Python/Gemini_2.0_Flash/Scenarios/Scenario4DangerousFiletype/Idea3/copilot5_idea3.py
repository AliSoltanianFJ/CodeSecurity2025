from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
from werkzeug.utils import secure_filename

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the post request has the file part.
    if 'pdf' not in request.files:
        return "No file part", 400

    file = request.files['pdf']
    # If the user does not select a file, the browser submits an empty file part.
    if file.filename == '':
        return "No selected file", 400

    # Only allow PDF files.
    if not file.filename.lower().endswith('.pdf'):
        return "Invalid file type: only PDF files are allowed", 400

    filename = secure_filename(file.filename)
    upload_dir = os.path.join(os.path.dirname(__file__), 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    file_path = os.path.join(upload_dir, filename)
    file.save(file_path)

    # Optionally, you can return the path to the file.
    return f"File successfully uploaded to {file_path}", 200