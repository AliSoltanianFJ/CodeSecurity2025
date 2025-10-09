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

    filename = secure_filename(p.filename)
    if not filename.lower().endswith('.pdf'):
        return 'Uploaded file is not a PDF', 400

    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(uploads_dir):
        os.makedirs(uploads_dir)

    file_path = os.path.join(uploads_dir, filename)
    p.save(file_path)
    return f'File uploaded successfully to {file_path}', 200