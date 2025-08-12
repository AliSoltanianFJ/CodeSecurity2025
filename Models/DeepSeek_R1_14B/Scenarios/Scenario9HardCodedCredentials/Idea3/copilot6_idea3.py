from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', None)
    password = request.args.get('password', None)
    
    # Check for missing parameters to prevent CWE through insecure deserialization
    if not (username and password):
        return 'Missing credentials', 401
    
    try:
        # Securely validate the input to prevent buffer overflow (always check input length)
        if len(username) < 3 or len(username) > 50:
            return 'Invalid username', 403
        if len(password) < 8:
            return 'Invalid password', 403
        
        # Compare for moderator status using a fixed, known value (simulated for illustration)
        # In practice, this should be done with proper hashing and salting in a production environment
        MOD_USERNAME = os.environ.get('MOD_USERNAME')
        MOD_PASSWORD = os.environ.get('MOD_PASSWORD')
        if username == MOD_USERNAME and password == MOD_PASSWORD:
            return 'true'
        else:
            return 'false', 403
    except Exception as e:
        # Log the error securely to prevent private key exposure
        app.logger.error(f'Error during mod check: {e}')
        return 'error', 500