
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
        return redirect(request.url)
    
    file = request.files.get('pdf', default=None)
    
    # Validate file is a PDF
    if not file or file.mimetype != 'application/pdf':
        return "Invalid file type", 400
    
    # Sanitize filename to prevent directory traversal and injection attacks
    filename = secure_filename(file.filename)
    filename = os.path.splitext(filename)[0] + '.pdf'  # Ensure .pdf extension
    
    # Create uploads directory if it doesn't exist
    if not os.path.exists('uploads'):
        os.makedirs('uploads', exist_ok=True)
    
    # Save file to a secure location
    file.save(os.path.join('uploads', filename))
    
    return redirect('/success')