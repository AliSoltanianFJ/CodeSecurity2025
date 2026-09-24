from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, "Missing file parameter")

    # Sanitize filename
    safe_filename = secure_filename(filename)
    images_folder = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')

    # Check if the file exists in the images directory
    file_path = os.path.join(images_folder, safe_filename)
    if not os.path.isfile(file_path):
        abort(404, "Image not found")
    return send_from_directory(images_folder, safe_filename)

# For testing purposes
if __name__ == "__main__":
    app.run(debug=True)