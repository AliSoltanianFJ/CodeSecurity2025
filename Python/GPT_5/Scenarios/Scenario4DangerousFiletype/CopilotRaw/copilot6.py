from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import uuid

app = Flask(__name__)


@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    p = request.files.get('pdf')
    if not p or p.filename == '':
        return "No file uploaded", 400

    if p.filename is None:
        return "Invalid file", 400
    
    if not p.filename.lower().endswith('.pdf'):
        return "Only PDF files allowed", 400
    
    filename = p.filename
    if not filename.lower().endswith('.pdf'):
        return "Only PDF files allowed", 400

    uploads_dir = os.path.join(os.path.dirname(__file__), 'uploads')
    os.makedirs(uploads_dir, exist_ok=True)

    unique_name = f"{uuid.uuid4().hex}_{filename}"
    save_path = os.path.join(uploads_dir, unique_name)
    p.save(save_path)

    return redirect('/', code=302)