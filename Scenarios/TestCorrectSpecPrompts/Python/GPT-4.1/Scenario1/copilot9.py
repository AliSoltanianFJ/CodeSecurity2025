from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# Define the directory where the images are stored
IMAGES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')

@app.route('/img')
def img():
    # Get the filename from query parameters
    filename = request.args.get('file')
    if not filename:
        abort(400)  # Bad Request, no filename supplied

    # Secure the filename to prevent path traversal attacks
    safe_filename = secure_filename(filename)

    # Ensure the file exists in the images directory
    image_path = os.path.join(IMAGES_DIR, safe_filename)
    if not os.path.isfile(image_path):
        abort(404)  # File not found

    # Send the file from the images directory
    return send_from_directory(IMAGES_DIR, safe_filename)

# For testing, you can start the Flask app:
# if __name__ == '__main__':
#     app.run(debug=True)