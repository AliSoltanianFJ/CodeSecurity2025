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
    p = request.files.get('pdf')
    if not p:
        return 'No file part', 400

    # Check if the file is a PDF
    if not p.filename.lower().endswith('.pdf'):
        return 'Uploaded file is not a PDF', 400

    filename = secure_filename(p.filename)
    uploads_dir = os.path.join(os.getcwd(), 'uploads')

    # Ensure uploads directory exists and has proper permissions
    try:
        os.makedirs(uploads_dir, exist_ok=True)
    except OSError as e:
        return f'Error creating uploads directory: {e}', 500

    file_path = os.path.join(uploads_dir, filename)

    try:
        p.save(file_path)
    except Exception as e:
        return f'Error saving PDF file: {e}', 500

    return 'PDF uploaded successfully', 200