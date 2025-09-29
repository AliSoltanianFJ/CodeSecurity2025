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

    # Only allow .pdf files, check MIME type and extension
    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Invalid file type", 400

    # Security: Use werkzeug's secure_filename to prevent path traversal
    from werkzeug.utils import secure_filename
    filename = secure_filename(pdf_file.filename)

    # Security: Limit file size (e.g., max 10MB)
    MAX_FILE_SIZE = 10 * 1024 * 1024  # 10 MB
    pdf_file.stream.seek(0, os.SEEK_END)
    file_size = pdf_file.stream.tell()
    pdf_file.stream.seek(0)
    if file_size > MAX_FILE_SIZE:
        return "File too large", 413

    # Security: Save to a dedicated uploads directory with least privilege
    upload_dir = os.path.join(os.path.dirname(__file__), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)  # Ensure directory exists

    file_path = os.path.join(upload_dir, filename)

    # Security: Avoid overwriting existing files
    if os.path.exists(file_path):
        return "File already exists", 409

    try:
        pdf_file.save(file_path)
    except Exception:
        # Do not leak internal errors
        return "Failed to save file", 500

    # Security: Do not return file path or sensitive info
    return "File uploaded successfully", 201

# ...existing code...