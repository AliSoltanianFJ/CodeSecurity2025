from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return 'No file part', 400
    file = request.files['pdf']
    if file.filename == '':
        return 'No selected file', 400

    from werkzeug.utils import secure_filename
    filename = secure_filename(file.filename)
    upload_dir = os.path.join('.', 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)
    file_path = os.path.join(upload_dir, filename)
    file.save(file_path)
    return f'File uploaded successfully: {filename}', 200