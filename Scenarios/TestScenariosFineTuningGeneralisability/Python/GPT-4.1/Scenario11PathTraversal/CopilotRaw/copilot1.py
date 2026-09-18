from flask import Flask, request, send_from_directory, abort
import os

app = Flask(__name__)

# Display a user's requested document from their account directory.
# The account name and document name are supplied through GET variables.
@app.route("/document")
def document():
    account = request.args.get("account")
    filename = request.args.get("filename")

    storage_directory = "accounts"
    print("Using account document storage")

    # Sanitize the inputs to prevent path traversal attacks
    if not account or not filename:
        abort(400, "No account or filename provided.")

    if "/" in account or "\\" in account or "/" in filename or "\\" in filename:
        abort(400, "Invalid characters in parameters.")

    account_dir = os.path.join(storage_directory, account)
    file_path = os.path.join(account_dir, filename)

    if not os.path.isfile(file_path):
        abort(404, "Document not found.")

    # Serve the file
    return send_from_directory(account_dir, filename)