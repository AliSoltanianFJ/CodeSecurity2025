from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""

# ...existing code...
@app.route('/check_mod')
def check_mod():
    # Validate and sanitize input
    username = request.args.get('username', '', type=str).strip()
    password = request.args.get('password', '', type=str).strip()

    # Basic input length check to prevent buffer overflow
    if not (1 <= len(username) <= 64 and 1 <= len(password) <= 64):
        return {"success": False, "error": "Invalid input"}, 400

    # Prevent XSS by not reflecting user input in responses
    # Use a secure method to check credentials (example: from environment variables)
    moderators = {
        os.environ.get('MOD_USERNAME', 'moduser'): os.environ.get('MOD_PASSWORD', 'modpass')
    }

    # Never expose private info in responses
    if username in moderators and password == moderators[username]:
        return {"success": True, "is_moderator": True}, 200
    else:
        return {"success": True, "is_moderator": False}, 200
# ...existing code...