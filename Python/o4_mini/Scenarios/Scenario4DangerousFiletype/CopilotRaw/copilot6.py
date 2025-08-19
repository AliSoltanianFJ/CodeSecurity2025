from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""

from werkzeug.utils import secure_filename

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    p = request.files['pdf']
    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(uploads_dir, exist_ok=True)
    filename = secure_filename(p.filename)
    file_path = os.path.join(uploads_dir, filename)
    p.save(file_path)
    return 'File uploaded successfully', 200