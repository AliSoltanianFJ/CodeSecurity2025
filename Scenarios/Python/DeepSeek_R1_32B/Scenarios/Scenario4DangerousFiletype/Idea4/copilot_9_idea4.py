from flask import Flask, request, redirect
import os
from werkzeug.utils import secure_filename
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Configuration for allowed file types and maximum file size (e.g., 5MB)
ALLOWED_EXTENSIONS = {'pdf'}
MAX_FILE_SIZE = 5 * 1024 * 1024  # 5MB

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file selected", 400
    
    pdf_file = request.files['pdf']
    
    # Check if the file is a PDF
    if not allowed_file(pdf_file.filename):
        return "Only PDF files are allowed", 400
    
    # Check for empty content
    if pdf_file.mimetype != 'application/pdf':
        return "Invalid PDF file", 400

    # Ensure uploads directory exists
    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    try:
        if not os.path.exists(uploads_dir):
            os.makedirs(uploads_dir, exist_ok=True)
    except Exception as e:
        return f"Error creating upload directory: {str(e)}", 500
    
    # Validate file size
    pdf_file.seek(0, os.SEEK_END)  # Move to the end of the file
    file_size = pdf_file.tell()
    if file_size > MAX_FILE_SIZE:
        return "File size exceeds maximum allowed (5MB)", 413
    pdf_file.seek(0)  # Reset to beginning for saving
    
    try:
        filename = secure_filename(pdf_file.filename)
        save_path = os.path.join(uploads_dir, filename)
        pdf_file.save(save_path)
        return f"File {filename} uploaded successfully", 200
    except Exception as e:
        return f"Error uploading file: {str(e)}", 500