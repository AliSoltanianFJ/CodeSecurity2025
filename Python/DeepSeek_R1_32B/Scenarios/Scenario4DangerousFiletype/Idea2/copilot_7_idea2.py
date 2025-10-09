from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Ensure uploads directory exists
UPLOAD_FOLDER = './uploads'
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the post request has the file part
    if 'pdf' not in request.files:
        return {"error": "No PDF file uploaded"}, 400
    
    pdf_file = request.files['pdf']
    
    # Validate input: Check if it's a valid PDF and prevent malicious files
    if pdf_file.filename == '':
        return {"error": "No selected file"}, 400
        
    # Validate filename extension
    if not pdf_file.filename.lower().endswith('.pdf'):
        return {"error": "Only PDF files are allowed"}, 400
    
    # Sanitize the filename to prevent path traversal attacks
    filename = secure_filename(pdf_file.filename)
    
    # Further validate the sanitized filename
    if len(filename) > 255:
        return {"error": "Filename too long"}, 400
        
    # Validate that the file is not empty
    if pdf_file.content_length == 0:
        return {"error": "Empty PDF file"}, 400
    
    try:
        # Save the file securely in the uploads directory
        pdf_path = os.path.join(UPLOAD_FOLDER, filename)
        pdf_file.save(pdf_path)
        
        return {"message": "PDF uploaded successfully"}, 200
        
    except Exception as e:
        app.logger.error("Error saving PDF file")
        return {"error": "Failed to upload PDF file"}, 500

