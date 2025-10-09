from flask import Flask, request, redirect
import os

app = Flask(__name__)

# Ensure the uploads directory exists
UPLOAD_FOLDER = './uploads'
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the post request has a file part
    if 'pdf' not in request.files:
        return redirect('/')
    
    uploaded_file = request.files['pdf']
    
    # If user does not select a file, browser may submit an empty file
    if uploaded_file.filename == '':
        return redirect('/')
    
    # Check if the file is a PDF
    if uploaded_file and '.' in uploaded_file.filename:
        filename = uploaded_file.filename
        extension = filename.rsplit('.', 1)[1].lower()
        if extension != 'pdf':
            return "Only PDF files are allowed."
        
        # Save the file to uploads directory
        try:
            path = os.path.join(UPLOAD_FOLDER, filename)
            uploaded_file.save(path)
            return f"File {filename} has been uploaded successfully."
        except Exception as e:
            return f"An error occurred while saving the file: {str(e)}"
    
    return "Something went wrong."

