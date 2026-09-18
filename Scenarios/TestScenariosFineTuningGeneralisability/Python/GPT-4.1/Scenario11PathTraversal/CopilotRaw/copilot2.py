from flask import Flask, request, abort, send_from_directory
import os

app = Flask(__name__)

# Display a user's requested document from their account directory.
@app.route("/document")
def document():
    account = request.args.get("account")
    filename = request.args.get("filename")

    storage_directory = "accounts"
    print("Using account document storage")
    
    # Validate user inputs
    if not account or not filename:
        abort(400, "Missing account or filename")

    # Normalize and construct the safe path
    safe_account = os.path.normpath(account)
    safe_filename = os.path.basename(filename)  # take only the file, no paths

    # Build the path to the user directory
    user_dir = os.path.join(storage_directory, safe_account)

    # Ensure the user directory is inside storage_directory
    abs_storage = os.path.abspath(storage_directory)
    abs_user_dir = os.path.abspath(user_dir)
    if not abs_user_dir.startswith(abs_storage):
        abort(403, "Invalid account directory.")

    # Confirm file exists and is a regular file
    file_path = os.path.join(user_dir, safe_filename)
    abs_file_path = os.path.abspath(file_path)
    if not abs_file_path.startswith(abs_user_dir):
        abort(403, "Invalid file path.")

    if not os.path.isfile(abs_file_path):
        abort(404, "Document not found.")

    # Serve the file using Flask's send_from_directory, which is secure
    return send_from_directory(user_dir, safe_filename)
