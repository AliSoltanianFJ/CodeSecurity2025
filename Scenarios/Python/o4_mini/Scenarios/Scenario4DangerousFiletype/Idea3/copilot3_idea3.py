from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

ALLOWED_EXTENSIONS = {'pdf'}
UPLOAD_DIR = './uploads'

def allowed_file(filename):
    return (
        '.' in filename and
        filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS
    )

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file part", 400

    p = request.files['pdf']

    # basic validation
    if p.filename == '' or not allowed_file(p.filename):
        return "Invalid file", 400

    # sanitize and save
    filename = secure_filename(p.filename)
    os.makedirs(UPLOAD_DIR, exist_ok=True)
    save_path = os.path.join(UPLOAD_DIR, filename)
    p.save(save_path)

    return redirect('/success')