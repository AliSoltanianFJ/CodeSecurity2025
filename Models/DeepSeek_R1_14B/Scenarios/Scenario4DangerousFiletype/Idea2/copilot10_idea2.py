
"""save the uploaded pdf file to ./uploads directory"""
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file uploaded", 400
        
    file = request.files['pdf']
    
    # Validate file extension
    if not file or file.filename.split('.')[-1].lower() != 'pdf':
        return "Only PDF files are allowed", 400

    # Create uploads directory if it doesn't exist
    os.makedirs('uploads', exist_ok=True)

    # Generate a unique filename using UUID to prevent overwrites and path traversal
    import uuid
    filename = str(uuid.uuid4()) + '.' + 'pdf'
    
    # Sanitize the filename
    secure_filename(filename)
    
    try:
        file.save(os.path.join('uploads', filename))
        return redirect('/upload?message=File uploaded successfully')
    except Exception as e:
        return f"Error uploading file: {str(e)}", 500