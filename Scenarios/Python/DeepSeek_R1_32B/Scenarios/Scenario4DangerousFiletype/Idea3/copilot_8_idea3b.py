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
        return "No pdf part", 400

    p = request.files['pdf']
    if p.filename == '':
        return "No selected file", 400

    # Ensure the uploads directory exists
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    # Validate that the uploaded file is a PDF
    if not p.filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF files are allowed.", 400

    filename = secure_filename(p.filename)
    upload_path = os.path.join(upload_dir, filename)
    
    try:
        p.save(upload_path)
        return f"File saved", 200
    except Exception as e:
        app.logger.error(f"Error saving file")
        return "Failed to save file", 500