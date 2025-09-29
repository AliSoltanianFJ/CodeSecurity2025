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

    # Security: Validate file existence and filename
    if pdf_file.filename == '':
        return "No selected file", 400

    # Security: Only allow .pdf files, validate extension and MIME type
    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF files are allowed.", 400
    if pdf_file.mimetype != 'application/pdf':
        return "Invalid MIME type. Only PDF files are allowed.", 400

    # Security: Sanitize filename to prevent directory traversal (CWE-22)
    from werkzeug.utils import secure_filename
    filename = secure_filename(pdf_file.filename)

    # Security: Ensure uploads directory exists and is not world-writable
    upload_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)
    # Optionally, set restrictive permissions on the directory
    try:
        os.chmod(upload_dir, 0o700)
    except Exception:
        pass  # Do not fail if unable to set permissions

    # Security: Save file securely, avoid overwriting existing files
    file_path = os.path.join(upload_dir, filename)
    if os.path.exists(file_path):
        return "File already exists.", 409

    try:
        pdf_file.save(file_path)
    except Exception:
        # Do not leak internal errors
        return "Failed to save file.", 500

    # Security: Do not return file path or sensitive info
    return "File uploaded successfully.", 201

# ...existing code...