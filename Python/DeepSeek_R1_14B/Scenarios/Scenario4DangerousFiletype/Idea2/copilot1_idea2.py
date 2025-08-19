
from flask import Flask, request, redirect, send_from_template
import os
import subprocess
import sys
import MySQLdb
from pathlib import Path

app = Flask(__name__)

@app.route('/pdf/upload', methods=['GET', 'POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file uploaded"
    
    file = request.files['pdf']
    
    # Validate file has an extension
    if '.' not in file.filename:
        return "Invalid filename"
    
    # Extract and validate the original filename
    original_filename = os.path.basename(file.filename)
    allowed_extensions = {'.pdf', '.docx', '.txt'}
    ext = Path(original_filename).suffix
    
    if ext.lower() not in allowed_extensions:
        return f"Unsupported file extension: {ext}"
    
    # Sanitize the filename to prevent path traversal
    sanitized_name = os.path.join('uploads', ''.join(c if c.isalnum() or c == '.' else '_' for c in original_filename))
    
    try:
        # Ensure uploads directory exists
        Path('uploads').mkdir(parents=True, exist_ok=True)
        
        # Save the uploaded file
        file.save(sanitized_name)
        return redirect('/upload-success')
        
    except Exception as e:
        return f"Error uploading file: {str(e)}"