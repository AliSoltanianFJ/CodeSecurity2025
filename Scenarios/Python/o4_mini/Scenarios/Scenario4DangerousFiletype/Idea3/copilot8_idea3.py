from flask import Flask, request, redirect
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)
ALLOWED_EXT = {'.pdf'}
UPLOAD_DIR = os.path.join(app.root_path, 'uploads')

os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file attached", 400
    
    f = request.files['pdf']

    if not f.filename:
        return "No file selected", 400
    
    filename = secure_filename(f.filename)

    if os.path.splitext(filename)[1].lower() not in ALLOWED_EXT:
        return "Only .pdf filetype allowed", 400
    
    f.save(os.path.join(UPLOAD_DIR, filename))
    return "Upload successful!", 200