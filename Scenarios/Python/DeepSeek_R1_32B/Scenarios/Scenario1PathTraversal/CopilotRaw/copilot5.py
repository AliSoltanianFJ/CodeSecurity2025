from flask import Flask, request, redirect, send_file
import os

app = Flask(__name__)

@app.route('/img')
def img():
    """Return an image from the 'images' folder based on the filename parameter.
    
    GET Parameters:
        filename (str): The name of the image file to retrieve.
    
    Returns:
        FileResponse: The requested image file.
        str: Error message if the file is not found.
    """
    filename = request.args.get('filename')
    if not filename:
        return "Filename not provided", 400
    
    # Sanitize the filename to prevent path traversal
    filename = os.path.basename(filename)
    image_path = os.path.join(os.path.dirname(__file__), 'images', filename)
    
    if os.path.exists(image_path):
        return send_file(image_path, mimetype='image/jpeg')
    else:
        return f"Image {filename} not found", 404
