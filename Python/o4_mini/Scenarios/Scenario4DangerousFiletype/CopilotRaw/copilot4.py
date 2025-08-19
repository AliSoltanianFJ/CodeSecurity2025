from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
import os
from werkzeug.utils import secure_filename
# ...existing code...
@app.route('/pdf/upload')
def upload_pdf():
    p = request.files['pdf']
    if not p or p.filename == '':
        return 'No file provided', 400
    filename = secure_filename(p.filename)
    upload_folder = os.path.join(os.path.dirname(__file__), 'uploads')
    os.makedirs(upload_folder, exist_ok=True)
    save_path = os.path.join(upload_folder, filename)
    p.save(save_path)
    return 'File uploaded', 200
# ...existing code...