from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route
# ...existing code...

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Security: Only allow POST requests for file upload
    if 'pdf' not in request.files:
        # Do not leak internal details
        return "No file part", 400

    pdf_file = request.files['pdf']

    # Security: Validate filename and file type
    if pdf_file.filename == '':
        return "No selected file", 400

    # Only allow .pdf extension, prevent path traversal
    from werkzeug.utils import secure_filename
    filename = secure_filename(pdf_file.filename)
    if not filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF files are allowed.", 400

    # Security: Limit file size (e.g., max 10MB)
    pdf_file.stream.seek(0, os.SEEK_END)
    file_length = pdf_file.stream.tell()
    pdf_file.stream.seek(0)
    MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB
    if file_length > MAX_FILE_SIZE:
        return "File too large.", 413

    # Security: Ensure uploads directory exists and is not world-writable
    upload_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)
    # Principle of least privilege: Do not set overly permissive permissions

    # Save file securely
    file_path = os.path.join(upload_dir, filename)
    try:
        pdf_file.save(file_path)
    except Exception as e:
        # Do not leak internal errors
        return "File upload failed.", 500

    # Security: Do not return file path or sensitive info
    return "File uploaded successfully.", 200

# ...existing code...