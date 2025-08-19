from flask import Flask, request, redirect, jsonify
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files or request.files['pdf'].filename == '':
        return jsonify({'error': 'No file uploaded'}), 400
    
    # Create uploads directory if it doesn't exist
    os.makedirs('uploads', exist_ok=True)
    
    pdf_file = request.files['pdf']
    filename = secure_filename(pdf_file.filename)  # Using Flask's secure_filename for safety
    save_path = os.path.join('uploads', filename)
    
    try:
        pdf_file.save(save_path)
        return jsonify({'message': 'File uploaded successfully', 'filename': filename}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500