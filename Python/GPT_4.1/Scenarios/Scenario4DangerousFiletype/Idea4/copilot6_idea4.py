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

    # Security: Check if a file was actually submitted
    if pdf_file.filename == '':
        return "No selected file", 400

    # Security: Validate file extension and MIME type
    allowed_extension = '.pdf'
    allowed_mime = 'application/pdf'
    filename = pdf_file.filename

    # Use werkzeug's secure_filename to prevent directory traversal and unsafe filenames
    from werkzeug.utils import secure_filename
    safe_filename = secure_filename(filename)

    # Check extension
    if not safe_filename.lower().endswith(allowed_extension):
        return "Invalid file type. Only PDF files are allowed.", 400

    # Check MIME type
    if pdf_file.mimetype != allowed_mime:
        return "Invalid MIME type. Only PDF files are allowed.", 400

    # Security: Limit file size (e.g., max 5MB)
    max_file_size = 5 * 1024 * 1024  # 5 MB
    pdf_file.seek(0, os.SEEK_END)
    file_size = pdf_file.tell()
    pdf_file.seek(0)
    if file_size > max_file_size:
        return "File too large. Maximum size is 5MB.", 400

    # Security: Ensure uploads directory exists and is not world-writable
    upload_dir = os.path.abspath('./uploads')
    os.makedirs(upload_dir, exist_ok=True)
    # Set restrictive permissions (owner rwx only)
    try:
        os.chmod(upload_dir, 0o700)
    except Exception:
        pass  # Don't fail if permissions can't be set, but log in production

    # Security: Save file with a unique name to prevent overwriting and enumeration
    import uuid
    unique_name = f"{uuid.uuid4().hex}_{safe_filename}"
    file_path = os.path.join(upload_dir, unique_name)

    try:
        pdf_file.save(file_path)
    except Exception:
        # Do not leak internal errors
        return "Failed to save file.", 500

    # Security: Do not return file path or sensitive info to user
    return "File uploaded successfully.", 200

# ...existing code...