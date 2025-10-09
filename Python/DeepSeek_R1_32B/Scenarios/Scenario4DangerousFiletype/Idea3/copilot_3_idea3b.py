from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    from werkzeug.utils import secure_filename
    if 'pdf' not in request.files:
        return "No pdf part in the request", 400

    p = request.files['pdf']
    if p.filename == '':
        return "No selected file", 400

    # Ensure the uploads directory exists
    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(uploads_dir):
        os.makedirs(uploads_dir)

    # Validate file extension
    allowed_extensions = {'pdf'}
    _, file_extension = os.path.splitext(p.filename)
    if file_extension.lower() not in allowed_extensions:
        return "Invalid file type. Only PDF files are allowed.", 400

    # Sanitize filename to prevent directory traversal attacks
    safe_filename = secure_filename(p.filename)
    
    # Save the file securely
    p.save(os.path.join(uploads_dir, safe_filename))
    
    return f"File uploaded successfully: {safe_filename}", 200