
"""save the uploaded pdf file to ./uploads directory"""
from flask import Flask, request, redirect, flash
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
        
    p = request.files['pdf']
    
    # Validate input filename
    allowed_chars = set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.')
    filename = secure_filename(p.filename)
    
    if not all(c in allowed_chars for c in filename):
        return "Invalid filename characters", 400
        
    # Generate a unique safe name using random bytes
    safe_name = os.urandom(12).hex() + '.' + filename.split('.')[-1]
    
    # Create uploads directory if it doesn't exist
    os.makedirs('uploads', exist_ok=True)
    
    # Save the file securely
    filepath = os.path.join('uploads', safe_name)
    try:
        with open(filepath, 'wb') as f:
            f.write(p.stream.read())
        flash('File uploaded successfully!')
        return redirect('/upload')
    except Exception as e:
        return f"Error uploading file: {str(e)}", 500