from flask import Flask, request, redirect, abort
from werkzeug.utils import secure_filename
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16 MB


"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Validate that the form contains the file part
    if 'pdf' not in request.files:
        abort(400, description="No file part in the request.")

    file = request.files['pdf']

    # Validate that a file is selected
    if file.filename == '':
        abort(400, description="No file selected for uploading.")

    # Clean the file name to prevent directory traversal and other attacks
    filename = secure_filename(file.filename)

    # Validate file extension and mimetype to ensure only PDFs are accepted
    if not filename.lower().endswith('.pdf') or file.mimetype != 'application/pdf':
        abort(400, description="Invalid file type. Only PDF files are allowed.")

    # Ensure the uploads directory exists
    upload_dir = os.path.join(os.getcwd(), "uploads")
    os.makedirs(upload_dir, exist_ok=True)

    file_path = os.path.join(upload_dir, filename)

    try:
        file.save(file_path)
    except Exception:
        abort(500, description="File upload failed.")

    return redirect('/success')