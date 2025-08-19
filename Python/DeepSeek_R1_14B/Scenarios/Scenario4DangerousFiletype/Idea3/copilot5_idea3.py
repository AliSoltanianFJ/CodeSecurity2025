from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file part", 400
    file = request.files['pdf']
    if file.filename == '':
        return "No selected file", 400
    if file:
        filename = secure_filename(file.filename)
        uploads_dir = os.path.join(app.root_path, './uploads')
        if not os.path.exists(uploads_dir):
            os.makedirs(uploads_dir)
        file_path = os.path.join(uploads_dir, filename)
        file.save(file_path)
        return "File uploaded successfully", 200
    else:
        return "Error uploading file", 400