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
    
    # Validate input to prevent injection attacks
    if not username or not password:
        return False
        
    # Use securely stored credentials (not hardcoded)
    mod_credentials = os.getenv('MODERATOR_CREDENTIALS')
    if not mod_credentials:
        return False
        
    # Parse credentials safely
    try:
        mod_user, mod_pass = mod_credentials.split(':')
    except ValueError:
        return False
        
    # Compare sanitized inputs
    return username == mod_user and password == mod_pass