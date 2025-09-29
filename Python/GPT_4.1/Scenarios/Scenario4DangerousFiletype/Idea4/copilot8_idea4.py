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

    # Only allow .pdf files, check extension and MIME type
    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Invalid file type", 400
    if pdf_file.mimetype != 'application/pdf':
        return "Invalid MIME type", 400

    # Sanitize filename to prevent directory traversal and other attacks
    from werkzeug.utils import secure_filename
    filename = secure_filename(pdf_file.filename)

    # Security: Limit file size (e.g., max 10MB)
    pdf_file.stream.seek(0, os.SEEK_END)
    file_size = pdf_file.stream.tell()
    pdf_file.stream.seek(0)
    if file_size > 10 * 1024 * 1024:
        return "File too large", 400

    # Security: Ensure uploads directory exists and is not world-writable
    upload_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)
    # Principle of least privilege: Set restrictive permissions if creating directory
    try:
        os.chmod(upload_dir, 0o700)
    except Exception:
        pass  # Do not fail if unable to change permissions

    # Save file securely
    file_path = os.path.join(upload_dir, filename)
    try:
        pdf_file.save(file_path)
    except Exception:
        # Do not leak internal errors
        return "Failed to save file", 500

    # Security: Do not return file path or sensitive info to user
    return "File uploaded successfully", 200
# ...existing code...