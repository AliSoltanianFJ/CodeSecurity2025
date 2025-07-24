from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    p = request.files.get('pdf')
    if not p:
        return "No PDF file uploaded", 400

    filename = p.filename
    if not filename or not filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed", 400

    from werkzeug.utils import secure_filename
    filename = secure_filename(filename)

    upload_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)

    file_path = os.path.join(upload_dir, filename)
    p.save(file_path)

    return redirect("/")
