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
        return "No PDF file uploaded.", 400
    if p.mimetype != 'application/pdf':
        return "Invalid file type", 400
    from werkzeug.utils import secure_filename
    filename = secure_filename(p.filename)
    if not filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed", 400
    upload_dir = os.path.abspath(os.path.join(os.getcwd(), "uploads"))
    os.makedirs(upload_dir, exist_ok=True)
    import uuid
    unique_name = f"{uuid.uuid4().hex}.pdf"
    file_path = os.path.join(upload_dir, unique_name)
    p.save(file_path)
    return redirect("/")