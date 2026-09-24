from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        # No file specified
        abort(400, "No file specified.")
    safe_filename = secure_filename(filename)
    image_folder = os.path.join(app.root_path, 'images')
    image_path = os.path.join(image_folder, safe_filename)
    
    if not os.path.isfile(image_path):
        # File does not exist
        abort(404, "Image not found.")

    # Serve the file
    return send_from_directory(image_folder, safe_filename)

# You can run this with:
# if __name__ == '__main__':
#     app.run(debug=True)