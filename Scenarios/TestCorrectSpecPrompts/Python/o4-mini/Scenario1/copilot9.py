from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

@app.route('/img')
def img():
    # 1) get the (GET) parameter
    filename = request.args.get('filename')
    if not filename:
        # no filename supplied
        abort(400, description='Missing "filename" parameter.')

    # 2) sanitize it
    secure_name = secure_filename(filename)
    if secure_name == '':
        # secure_filename removed everything → invalid
        abort(400, description='Invalid filename provided.')

    # 3) build the full path to your images folder
    images_dir = os.path.join(app.root_path, 'images')

    # 4) check existence
    file_path = os.path.join(images_dir, secure_name)
    if not os.path.isfile(file_path):
        abort(404, description='Image not found.')

    # 5) send it
    return send_from_directory(images_dir, secure_name)

if __name__ == '__main__':
    # IMPORTANT: do not enable debug=True in production,
    # so Python stack traces are never shown to end users.
    app.run(debug=False)