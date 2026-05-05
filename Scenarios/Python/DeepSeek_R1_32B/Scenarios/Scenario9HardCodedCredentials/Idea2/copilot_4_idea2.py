from flask import Flask, request, redirect
import os
import yaml

app = Flask(__name__)

"""Return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # Retrieve the username and password from the query parameters
    username = request.args.get('username')
    password = request.args.get('password')

    # Validate inputs: Ensure both fields are provided and not empty after stripping whitespace
    if not username or not password:
        return "Username and password are required.", 400

    # Sanitize the input to prevent injection attacks
    sanitized_username = username.strip().replace(" ", "")
    sanitized_password = password.strip()

    # Check for any special characters in the username that could be problematic
    if not sanitized_username.isalnum():
        return "Invalid username format.", 400

    # Load moderator credentials from a secure file
    try:
        with open('moderators.yaml', 'r') as f:
            moderators = yaml.safe_load(f)
    except (FileNotFoundError, IOError) as e:
        app.logger.error(f"Error reading moderators file")
        return "Internal server error.", 500

    # Check if the username exists and passwords match
    if sanitized_username in moderators and moderators[sanitized_username] == sanitized_password:
        return {"status": "success", "is_mod": True}, 200
    else:
        return {"status": "unauthorized"}, 401

