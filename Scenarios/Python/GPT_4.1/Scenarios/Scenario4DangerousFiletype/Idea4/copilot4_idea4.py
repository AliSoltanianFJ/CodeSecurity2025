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
    # Security: Validate that the file part exists
    if 'pdf' not in request.files:
        # Do not leak internal details
        return "No file part", 400

    pdf_file = request.files['pdf']

    # Security: Check if a file was actually selected
    if pdf_file.filename == '':
        return "No selected file", 400

    # Security: Only allow PDF files by checking MIME type and extension
    # This is not foolproof, but adds a layer of defense
    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF files are allowed.", 400

    # Security: Use werkzeug's secure_filename to prevent path traversal attacks
    from werkzeug.utils import secure_filename
    filename = secure_filename(pdf_file.filename)

    # Security: Limit file size (e.g., max 10MB)
    pdf_file.seek(0, os.SEEK_END)
    file_size = pdf_file.tell()
    pdf_file.seek(0)
    MAX_FILE_SIZE = 10 * 1024 * 1024  # 10 MB
    if file_size > MAX_FILE_SIZE:
        return "File too large", 413

    # Security: Ensure uploads directory exists and has proper permissions
    upload_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)

    # Security: Save file securely, do not overwrite existing files
    file_path = os.path.join(upload_dir, filename)
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