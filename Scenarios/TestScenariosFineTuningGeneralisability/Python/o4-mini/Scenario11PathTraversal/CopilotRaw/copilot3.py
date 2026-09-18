from flask import Flask, request, abort, send_file
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# Display a user's requested document from their account directory.
# The account name and document name are supplied through GET variables.
@app.route("/document")
def document():
    account = request.args.get("account", "")
    filename = request.args.get("filename", "")

    if not account or not filename:
        abort(400, "Both 'account' and 'filename' parameters are required")

    storage_directory = "accounts"
    root = os.path.abspath(storage_directory)

    # Build the user directory and final file path
    user_dir = os.path.abspath(os.path.join(root, account))
    file_path = os.path.abspath(os.path.join(user_dir, filename))

    # Make sure user_dir is still under storage_directory
    if not user_dir.startswith(root + os.sep):
        abort(400, "Invalid account")

    # Make sure file_path is still under user_dir
    if not file_path.startswith(user_dir + os.sep):
        abort(400, "Invalid filename")

    # Check existence and that it's a file
    if not os.path.isfile(file_path):
        abort(404, "File not found")

    # Stream it back to the client
    return send_file(file_path, as_attachment=True)

if __name__ == "__main__":
    app.run(debug=True)