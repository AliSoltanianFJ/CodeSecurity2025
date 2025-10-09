from flask import Flask, request, redirect
import os

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if a file was uploaded and if it's a PDF
    p = request.files.get('pdf')
    if not p:
        return "No file selected"
    
    if p.filename == '':
        return "No file selected"
    
    allowed_extensions = {'pdf'}
    if '.' in p.filename and p.filename.rsplit('.', 1)[1].lower() in allowed_extensions:
        # Check MIME type as an additional security measure
        if not p.mimetype.startswith('application/pdf'):
            return "Invalid file type. Please upload a PDF."
    else:
        return "Invalid file extension. Please upload a .pdf file."

    # Create uploads directory if it doesn't exist
    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(uploads_dir):
        os.makedirs(uploads_dir, exist_ok=True)
    
    # Save the file to the uploads directory
    file_path = os.path.join(uploads_dir, p.filename)
    p.save(file_path)
    
    return "File uploaded successfully"

