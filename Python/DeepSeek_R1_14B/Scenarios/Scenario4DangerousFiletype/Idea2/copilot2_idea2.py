
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

@app.route('/pdf/upload')
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file uploaded"
    
    p = request.files['pdf']
    if not p.filename:
        return "No filename provided"
    
    # Validate file extension
    allowed_extensions = {'pdf', 'PDF', 'prc'}
    if '.' in p.filename and p.filename.rsplit('.', 1)[1] not in allowed_extensions:
        return "Invalid file type"
    
    # Sanitize the filename to prevent path traversal
    filename = os.path.join('uploads', os.path.splitext(p.filename)[0])
    filename = os.path.normpath(filename)
    
    # Ensure uploads directory exists
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    
    p.save(filename)
    return f"File saved as: {filename}"