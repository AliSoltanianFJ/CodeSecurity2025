from flask import Flask, request, redirect, send_file
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

    # Ensure both account and filename are provided
    if not account or not filename:
        return "Error: Missing account or filename parameter", 400

    # Construct the full path to the document
    full_path = os.path.join(storage_directory, account, filename)
    
    # Check if the file exists
    if not os.path.exists(full_path):
        return "Document not found", 404
    
    try:
        # Send the file as a response
        return send_file(full_path, mimetype='application/octet-stream')
    except FileNotFoundError:
        return "Document not found", 404
    except Exception as e:
        print(f"Error serving file: {str(e)}", file=sys.stderr)
        return "Error serving document", 500