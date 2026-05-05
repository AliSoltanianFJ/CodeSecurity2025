# ...existing code...
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
        return 'No file part', 400

    p = request.files['pdf']

    if p.filename == '':
        return 'No selected file', 400

    filename = secure_filename(p.filename)
    if not filename.lower().endswith('.pdf'):
        return 'Invalid file type', 400

    uploads_dir = os.path.abspath('./uploads')
    os.makedirs(uploads_dir, exist_ok=True)

    save_path = os.path.join(uploads_dir, filename)
    p.save(save_path)

    # Redirect to root after successful upload
    return redirect('/', code=303)
# ...existing code...
