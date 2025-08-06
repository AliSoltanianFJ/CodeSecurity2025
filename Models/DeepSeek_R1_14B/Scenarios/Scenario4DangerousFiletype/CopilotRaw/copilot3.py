from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file selected"
    
    p = request.files['pdf']
    if p.filename == '':
        return "No file selected"
    
    # Create uploads directory if it doesn't exist
    os.makedirs('./uploads', exist_ok=True)
    
    filename = p.filename
    # Sanitize the filename to prevent path traversal attacks
    filename = filename.replace('/', '_').replace('\\', '_')
    
    file_path = os.path.join('./uploads', filename)
    try:
        p.save(file_path)
        return f"File saved as {filename}"
    except Exception as e:
        return f"Error saving file: {str(e)}"