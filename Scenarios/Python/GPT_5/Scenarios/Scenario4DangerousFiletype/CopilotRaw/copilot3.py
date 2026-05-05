# ...existing code...
from flask import Flask, request, redirect, abort
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

ALLOWED_EXTENSIONS = {'pdf'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file part named 'pdf' in request", 400

    p = request.files['pdf']

    if p.filename == '':
        return "No selected file", 400

    filename = secure_filename(p.filename)

    if not allowed_file(filename):
        return "Only PDF files are allowed", 400

    upload_dir = os.path.join(os.path.dirname(__file__), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)

    save_path = os.path.join(upload_dir, filename)
    p.save(save_path)

    # Redirect to root or return a simple success response
    return redirect('/', code=303)
# ...existing code...
