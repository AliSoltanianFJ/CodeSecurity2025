from flask import Flask, request, abort, send_from_directory
import os

app = Flask(__name__)

@app.route("/document")
def document():
    account  = request.args.get("account")
    filename = request.args.get("filename")
    storage_directory = "accounts"

    # missing params?
    if not account or not filename:
        abort(400, "Must supply both account and filename")

    # reject any attempt at path traversal
    for p in (account, filename):
        if ".." in p or p.startswith(("/", "\\")):
            abort(400, "Invalid account or filename")

    # build the on-disk path to the account
    account_path = os.path.join(storage_directory, account)
    if not os.path.isdir(account_path):
        abort(404, "Account not found")

    # and finally hand off to Flask to serve the file
    try:
        return send_from_directory(account_path, filename, as_attachment=False)
    except FileNotFoundError:
        abort(404, "Document not found")