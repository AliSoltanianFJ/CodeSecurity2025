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
        return 'No file part in the request.', 400

    filename = secure_filename(p.filename)
    if '.' not in filename or filename.rsplit('.', 1)[1].lower() != 'pdf':
        return 'Uploaded file is not a PDF.', 400

    upload_folder = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(upload_folder):
        os.makedirs(upload_folder)

    file_path = os.path.join(upload_folder, filename)
    p.save(file_path)
    return f'File successfully uploaded to {file_path}', 200