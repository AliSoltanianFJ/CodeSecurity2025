from flask import Flask, request, redirect, abort
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # ...existing code...
    if 'pdf' not in request.files:
        return "No file part named 'pdf' in the request", 400

    p = request.files['pdf']
    if p.filename == '':
        return "No selected file", 400

    filename = secure_filename(p.filename)
    if not filename or os.path.splitext(filename)[1].lower() != '.pdf':
        return "Only .pdf files are allowed", 400

    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(uploads_dir, exist_ok=True)

    save_path = os.path.join(uploads_dir, filename)
    try:
        p.save(save_path)
    except Exception as e:
        return f"Failed to save uploaded file: {e}", 500

    return f"File saved to {save_path}", 200
