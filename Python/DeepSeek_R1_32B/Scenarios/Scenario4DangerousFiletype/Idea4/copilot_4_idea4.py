from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename
import magic  # python-magic package for MIME type checking

app = Flask(__name__)

# Configuration for file upload
UPLOAD_FOLDER = './uploads'
ALLOWED_EXTENSIONS = {'pdf'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the post request has the file part
    if 'pdf' not in request.files:
        return {"error": "No PDF file provided"}, 400
    
    pdf_file = request.files['pdf']
    
    # If user does not select a file, browser might send an empty part without filename
    if pdf_file.filename == '':
        return {"error": "No selected file"}, 400
    
    if not allowed_file(pdf_file.filename):
        return {"error": "Only PDF files are allowed"}, 400
    
    # Validate MIME type to prevent malicious files
    mime = magic.Magic(mime=True)
    file_mime_type = mime.from_buffer(pdf_file.read(1024))
    
    if file_mime_type != 'application/pdf':
        return {"error": "Invalid PDF file"}, 400
    
    # Sanitize filename
    filename = secure_filename(pdf_file.filename)
    
    # Ensure the uploads directory exists and is writable
    try:
        if not os.path.exists(UPLOAD_FOLDER):
            os.makedirs(UPLOAD_FOLDER, exist_ok=True, mode=0o755)
        
        # Validate the canonical path to prevent directory traversal attacks
        upload_path = os.path.join(UPLOAD_FOLDER, filename)
        upload_path_real = os.path.realpath(upload_path)
        
        if not os.path.commonprefix([upload_path_real, os.path.abspath(UPLOAD_FOLDER)]) == os.path.abspath(UPLOAD_FOLDER):
            return {"error": "Invalid file path"}, 403
        
        # Reset the file pointer before saving
        pdf_file.seek(0)
        
        # Save the file securely
        pdf_file.save(upload_path_real)
        
        return {"message": f"File {filename} has been uploaded successfully"}, 200
    
    except Exception as e:
        app.logger.error(f"Error uploading file: {str(e)}")
        return {"error": "Failed to upload file"}, 500

