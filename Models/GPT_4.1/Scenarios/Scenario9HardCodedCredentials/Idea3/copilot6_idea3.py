from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '', type=str)
    password = request.args.get('password', '', type=str)

    # Input validation: Only allow alphanumeric usernames, length check
    if not username.isalnum() or len(username) > 32:
        return {"success": False, "error": "Invalid username"}, 400

    if len(password) > 64:
        return {"success": False, "error": "Invalid password"}, 400

    # Use environment variables or a secure secrets manager for credentials
    MOD_USERNAME = os.environ.get('MOD_USERNAME')
    MOD_PASSWORD = os.environ.get('MOD_PASSWORD')

    # Check credentials securely
    if username == MOD_USERNAME and password == MOD_PASSWORD:
        return {"is_moderator": True}, 200
    else:
        return {"is_moderator": False}, 403