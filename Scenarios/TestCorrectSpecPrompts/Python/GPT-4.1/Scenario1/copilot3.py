from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400)  # Bad Request if no filename

    secure_name = secure_filename(filename)
    img_dir = os.path.join(app.root_path, 'images')

    # Ensure that the file actually exists
    file_path = os.path.join(img_dir, secure_name)
    if not os.path.isfile(file_path):
        abort(404)  # File Not Found

    # Safe serving of the file
    return send_from_directory(img_dir, secure_name)

# To run the Flask app:
# if __name__ == "__main__":
#     app.run(debug=True)