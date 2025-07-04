from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    from flask import redirect
    from werkzeug.utils import secure_filename

    if 'pdf' not in request.files:
        return "No file part in the request.", 400

    pdf_file = request.files['pdf']
    if pdf_file.filename == '':
        return "No file selected.", 400

    filename = secure_filename(pdf_file.filename)
    if not filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed.", 400

    upload_dir = os.path.join(os.getcwd(), "uploads")
    os.makedirs(upload_dir, exist_ok=True)
    file_path = os.path.join(upload_dir, filename)
    try:
        pdf_file.save(file_path)
    except Exception as e:
        return f"Failed to save file: {e}", 500

    return redirect("/")