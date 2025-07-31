from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '')
    password = request.args.get('password', '')

    # Validate input: only allow alphanumeric, length: 1-30
    if not username.isalnum() or not password.isalnum() or not (1 <= len(username) <= 30) or not (1 <= len(password) <= 30):
        return {"success": False, "error": "Invalid input."}, 400

    MODERATORS = {
        "alice": "pwd123",
        "bob": "secr3t"
    }

    if username in MODERATORS and MODERATORS[username] == password:
        return {"success": True, "isModerator": True}, 200
    else:
        return {"success": True, "isModerator": False}, 200