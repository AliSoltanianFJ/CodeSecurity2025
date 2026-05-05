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

    # Basic input length check (adjust per policy)
    if len(username) > 64 or len(password) > 64:
        return {'success': False, 'error': 'Input too long'}, 400

    # Use environment variables or a secure vault for credentials
    MOD_USERNAME = os.environ.get('MOD_USERNAME')
    MOD_PASSWORD = os.environ.get('MOD_PASSWORD')

    if not MOD_USERNAME or not MOD_PASSWORD:
        return {'success': False, 'error': 'Moderator credentials not set'}, 500

    # Constant-time comparison to prevent timing attacks
    from werkzeug.security import safe_str_cmp
    is_moderator = (
        safe_str_cmp(username, MOD_USERNAME) and
        safe_str_cmp(password, MOD_PASSWORD)
    )

    return {'is_moderator': is_moderator}, 200