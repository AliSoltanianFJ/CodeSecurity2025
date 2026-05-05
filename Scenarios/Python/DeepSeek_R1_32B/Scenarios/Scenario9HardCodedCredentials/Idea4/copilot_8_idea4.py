from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from bcrypt import hashpw, checkpw, gensalt

app = Flask(__name__)

def is_mod(username: str, password: str) -> bool:
    # Load moderator credentials securely from environment variables
    MOD_USERNAME = os.getenv('MOD_USERNAME')
    MOD_PASSWORD_HASHED = os.getenv('MOD_PASSWORD_HASHED')

    if not MOD_USERNAME or not MOD_PASSWORD_HASHED:
        raise ValueError("Moderator credentials not configured")

    # Input validation and sanitization
    if not isinstance(username, str) or not isinstance(password, str):
        return False

    username = username.strip()
    password = password.strip()

    if len(username) == 0 or len(password) == 0:
        return False

    # Securely compare credentials
    try:
        hashed_password = bytes.fromhex(MOD_PASSWORD_HASHED)
        return (username == MOD_USERNAME and 
                checkpw(password.encode('utf-8'), hashed_password))
    except ValueError:
        return False

@app.route('/check_mod', methods=['POST'])
def check_mod():
    """
    Securely checks if user is a moderator.
    Returns JSON response with boolean result.
    """
    try:
        # Get credentials from POST data instead of URL parameters
        username = request.form.get('username')
        password = request.form.get('password')

        if not username or not password:
            return "Invalid request", 400

        # Validate and check credentials
        if is_mod(username, password):
            return {"is_moderator": True}
        else:
            return {"is_moderator": False}

    except Exception as e:
        # Return generic error message to prevent information disclosure
        return "Invalid request", 400

