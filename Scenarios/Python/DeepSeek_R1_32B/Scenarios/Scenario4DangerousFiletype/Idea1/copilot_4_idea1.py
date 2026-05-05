from flask import Flask, request, redirect
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the request contains a file
    pdf_file = request.files.get('pdf')
    if not pdf_file or pdf_file.filename == '':
        return "No PDF file uploaded.", 400

    # Validate that it's a PDF file
    filename = secure_filename(pdf_file.filename)
    if not filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed.", 400

    # Ensure the uploads directory exists
    upload_dir = os.path.join(os.getcwd(), "uploads")
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir, exist_ok=True)

    # Save the file to the uploads directory with a secure filename
    file_path = os.path.join(upload_dir, filename)
    pdf_file.save(file_path)

    return redirect('/')

