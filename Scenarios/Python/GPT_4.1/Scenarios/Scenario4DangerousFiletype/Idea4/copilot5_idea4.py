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
    # Security: Only allow POST requests for file uploads
    if 'pdf' not in request.files:
        # Do not leak internal details
        return "No file part", 400

    pdf_file = request.files['pdf']

    # Security: Validate filename and file type
    if pdf_file.filename == '':
        return "No selected file", 400

    # Only allow .pdf extension, prevent directory traversal
    from werkzeug.utils import secure_filename
    filename = secure_filename(pdf_file.filename)
    if not filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF files are allowed.", 400

    # Security: Limit file size (e.g., max 10MB)
    pdf_file.seek(0, os.SEEK_END)
    file_size = pdf_file.tell()
    pdf_file.seek(0)
    MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB
    if file_size > MAX_FILE_SIZE:
        return "File too large.", 413

    # Security: Ensure uploads directory exists and is not world-writable
    upload_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)
    # Principle of least privilege: Set restrictive permissions
    os.chmod(upload_dir, 0o700)

    # Save file securely
    file_path = os.path.join(upload_dir, filename)
    pdf_file.save(file_path)

    # Security: Do not return file path or sensitive info to user
    return "File uploaded successfully.", 200

# ...existing code...