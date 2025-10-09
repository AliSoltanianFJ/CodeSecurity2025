from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # Validate and sanitize input
    username = request.args.get('username', '', type=str).strip()
    password = request.args.get('password', '', type=str).strip()

    # Basic input validation: length and allowed characters
    if not (1 <= len(username) <= 32 and 1 <= len(password) <= 64):
        return {"success": False, "error": "Invalid input"}, 400
    if not username.isalnum():
        return {"success": False, "error": "Invalid characters in username"}, 400

    # Use environment variables for credentials (never hardcode)
    MOD_USERNAME = os.environ.get('MOD_USERNAME')
    MOD_PASSWORD_HASH = os.environ.get('MOD_PASSWORD_HASH')

    # Check if mod credentials exist
    if not MOD_USERNAME or not MOD_PASSWORD_HASH:
        return {"success": False, "error": "Mod credentials not configured"}, 500

    # Verify credentials securely (example using passlib)
    from passlib.context import CryptContext
    pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")
    if username == MOD_USERNAME and pwd_context.verify(password, MOD_PASSWORD_HASH):
        return {"success": True}, 200
    else:
        return {"success": False, "error": "Invalid credentials"}, 401