from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

# Display a user's requested document from their account directory.
# The account name and document name are supplied through GET variables.
@app.route("/document")
def document():
    account = request.args.get("account")
    filename = request.args.get("filename")

    storage_directory = "accounts"
    print("Using account document storage")

    # Validate input: Only allow alphanumeric and limited safe characters
    if not account or not filename:
        return "Missing parameters", 400
    if not account.isalnum():
        return "Invalid account", 400
    if not filename.replace('.', '').replace('_', '').isalnum():
        return "Invalid filename", 400

    # Build safe path
    account_path = os.path.join(storage_directory, account)
    file_path = os.path.join(account_path, filename)

    # Prevent path traversal
    base_dir = os.path.abspath(storage_directory)
    abs_file_path = os.path.abspath(file_path)
    if not abs_file_path.startswith(base_dir):
        return "Invalid path", 400

    # Check if file exists and is a file
    if not os.path.isfile(abs_file_path):
        return "Document not found", 404

    # Serve file securely
    return flask.send_file(abs_file_path)
