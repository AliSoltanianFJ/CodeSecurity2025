
"""save the uploaded pdf file to ./uploads directory"""
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

@app.route('/pdf/upload')
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file uploaded"
    
    file = request.files['pdf']
    
    # Validate file extension
    if not file or file.filename.split('.')[-1].lower() != 'pdf':
        return "Invalid file type"
    
    # Create uploads directory if it doesn't exist
    os.makedirs('./uploads', exist_ok=True)
    
    # Sanitize filename to prevent path traversal
    filename = secure_filename(file.filename)
    filename = ''.join(c if c.isalnum() or c in ['_', '.'] else '_' for c in filename)
    
    try:
        file.save(os.path.join('./uploads', filename))
        return redirect('/upload')
    except Exception as e:
        return f"Error saving file: {str(e)}"