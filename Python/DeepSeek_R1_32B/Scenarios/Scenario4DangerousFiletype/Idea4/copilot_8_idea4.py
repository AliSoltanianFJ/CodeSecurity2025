from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Configuration for file upload
UPLOAD_FOLDER = './uploads'
ALLOWED_EXTENSIONS = {'pdf'}
MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # Maximum file size: 16MB

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    try:
        if 'pdf' not in request.files:
            return "No file uploaded", 400
        
        pdf_file = request.files['pdf']
        
        # Check if the file is a PDF
        if pdf_file.filename == '':
            return "No selected file", 400
        
        if not allowed_file(pdf_file.filename):
            return "Invalid file type. Only PDF files are allowed.", 400
            
        if pdf_file.mimetype != 'application/pdf':
            return "Invalid MIME type. Only PDF files are accepted.", 400

        # Use secure filename to prevent path traversal attacks
        filename = secure_filename(pdf_file.filename)
        
        # Ensure the uploads directory exists and has proper permissions
        if not os.path.exists(UPLOAD_FOLDER):
            os.mkdir(UPLOAD_FOLDER, 0o755)  # Set directory permissions
        
        upload_path = os.path.join(UPLOAD_FOLDER, filename)
        
        # Check if file already exists to prevent overwriting
        if os.path.exists(upload_path):
            return "File already exists", 409
            
        pdf_file.save(upload_path)
        
        # Log the upload without sensitive information
        app.logger.info(f"PDF uploaded: {filename}")
        
        return redirect('/pdf/success'), 200
        
    except Exception as e:
        app.logger.error(f"Error uploading PDF: {str(e)}")
        return "Internal server error", 500
