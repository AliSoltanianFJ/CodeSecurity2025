from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
from flask import send_from_directory, abort
from werkzeug.utils import secure_filename

# Ensure the upload directory exists
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No pdf part in the request", 400

    file = request.files['pdf']

    # Sanitize filename
    filename = secure_filename(file.filename)

    # Ensure filename has a pdf extension
    if not filename.lower().endswith('.pdf'):
        return "Invalid file extension", 400

    file_path = os.path.join(UPLOAD_DIR, filename)

    # Save the file
    file.save(file_path)

    return f"File '{filename}' uploaded successfully", 200