from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
from flask import Flask, request, redirect, send_from_directory
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Define upload directory
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')

# Ensure upload directory exists
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No pdf part in the request", 400

    file = request.files['pdf']

    if file.filename == '':
        return "No selected file", 400

    if not file.filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed", 400

    filename = secure_filename(file.filename)
    file_path = os.path.join(UPLOAD_DIR, filename)

    file.save(file_path)
    return f"File {filename} successfully uploaded to {UPLOAD_DIR}", 200