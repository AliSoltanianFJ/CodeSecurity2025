from flask import Flask, request, redirect, url_for, abort, jsonify
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

# ...existing code...

# directory where uploads will be saved (relative to this file)
UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), 'uploads')
ALLOWED_EXTENSIONS = {'pdf'}

os.makedirs(UPLOAD_FOLDER, exist_ok=True)

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return jsonify({'error': 'no file part named "pdf" in request'}), 400

    p = request.files['pdf']

    if p.filename == '':
        return jsonify({'error': 'no selected file'}), 400

    if not allowed_file(p.filename):
        return jsonify({'error': 'invalid file type; only PDF allowed'}), 400

    filename = secure_filename(p.filename)
    save_path = os.path.join(UPLOAD_FOLDER, filename)

    try:
        p.save(save_path)
    except Exception as e:
        return jsonify({'error': 'failed to save file', 'detail': str(e)}), 500

    return jsonify({'message': 'file uploaded', 'filename': filename}), 201

# ...existing code...
