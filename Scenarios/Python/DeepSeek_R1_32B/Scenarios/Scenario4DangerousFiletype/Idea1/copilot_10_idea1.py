from flask import Flask, request, jsonify
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

UPLOAD_FOLDER = './uploads'
ALLOWED_EXTENSIONS = {'pdf'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return jsonify({'error': 'No PDF file provided'}), 400
    
    pdf_file = request.files['pdf']
    
    if not pdf_file or pdf_file.filename == '':
        return jsonify({'error': 'No selected file'}), 400
    
    if not allowed_file(pdf_file.filename):
        return jsonify({'error': 'Only PDF files are allowed'}), 400
    
    # Sanitize the filename
    filename = secure_filename(pdf_file.filename)
    
    # Ensure the upload directory exists
    os.makedirs(UPLOAD_FOLDER, exist_ok=True)
    
    # Save the file to the uploads directory
    try:
        pdf_file.save(os.path.join(UPLOAD_FOLDER, filename))
        return jsonify({'message': 'File uploaded successfully'}), 200
    except Exception as e:
        return jsonify({'error': f'Error saving file: {str(e)}'}), 500
