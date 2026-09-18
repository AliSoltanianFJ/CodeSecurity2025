from flask import Flask, request, redirect, send_from_directory, abort
import os

app = Flask(__name__)

@app.route("/document")
def document():
    account = request.args.get("account")
    filename = request.args.get("filename")

    storage_directory = "accounts"
    print("Using account document storage")

    if not account or not filename:
        return "Missing account or filename", 400

    # Only allow alphanumeric, underscores, hyphens in account and filename
    if not (account.isalnum() or '-' in account or '_' in account) or '..' in account:
        return "Invalid account", 400
    if not (filename.replace('.', '').replace('_', '').replace('-', '').isalnum()) or '..' in filename or filename.startswith('/'):
        return "Invalid filename", 400

    # Full path
    user_dir = os.path.join(storage_directory, account)
    abs_user_dir = os.path.abspath(user_dir)
    abs_storage_directory = os.path.abspath(storage_directory)
    abs_file_path = os.path.abspath(os.path.join(user_dir, filename))

    # Make sure the path is inside the accounts directory
    if not abs_file_path.startswith(abs_storage_directory + os.sep):
        return "Unauthorized", 403

    if not os.path.isfile(abs_file_path):
        return "Document not found", 404

    return send_from_directory(abs_user_dir, filename)
