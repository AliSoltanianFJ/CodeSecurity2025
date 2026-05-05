from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename
import mimetypes

app = Flask(__name__)

UPLOAD_FOLDER = './uploads'
MAX_FILE_SIZE = 1024 * 1024  # Maximum file size of 1MB

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    try:
        if 'pdf' not in request.files:
            return jsonify({'error': 'No PDF file uploaded'}), 400

        pdf_file = request.files['pdf']
        
        # Check if the filename is empty
        if pdf_file.filename == '':
            return jsonify({'error': 'Empty filename'}), 400
            
        # Validate file type and extension
        if not pdf_file.filename.lower().endswith('.pdf'):
            return jsonify({'error': 'Only PDF files are allowed'}), 400
            
        # Verify MIME type is application/pdf
        mime_type = mimetypes.guess_type(pdf_file.filename)[0]
        if mime_type != 'application/pdf':
            return jsonify({'error': 'Invalid file type - must be PDF'}), 400

        # Check if the file size exceeds maximum allowed size
        if pdf_file.content_length > MAX_FILE_SIZE:
            return jsonify({'error': f'File too large. Maximum size is {MAX_FILE_SIZE/1024} KB'}), 400
            
        # Sanitize the filename to prevent path traversal attacks
        filename = secure_filename(pdf_file.filename)
        
        # Create uploads directory if it doesn't exist
        os.makedirs(UPLOAD_FOLDER, exist_ok=True)
        
        # Save the file securely
        file_path = os.path.join(UPLOAD_FOLDER, filename)
        pdf_file.save(file_path)
        
        return jsonify({'message': 'PDF uploaded successfully', 
                        'filename': filename}), 200

    except Exception as e:
        return jsonify({'error': 'Error occurred while uploading PDF'}), 500

