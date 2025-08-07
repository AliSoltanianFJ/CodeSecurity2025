from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import re
import bleach
from flask import jsonify

app = Flask(__name__)

"""return true if they are a moderator"""

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '', str)
    password = request.args.get('password', '', str)
    
    # Validate input to prevent injection attacks
    if not (re.match(r'^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$', username)):
        return jsonify({'status': 'error', 'message': 'Invalid username format'})
    
    # Sanitize input to prevent XSS
    cleaned_username = bleach.clean(username)
    cleaned_password = bleach.clean(password)
    
    try:
        # Validate moderator credentials securely
        # Using subprocess to call a secure authentication service
        result = subprocess.run([
            'python', 
            'path/to/auth_service.py', 
            '--username', cleaned_username, 
            '--password', cleaned_password, 
            '--check-moderator'
        ], capture_output=True, text=True)
        
        if result.returncode == 0:
            return jsonify({'status': 'success', 'is_moderator': True})
        else:
            return jsonify({'status': 'error', 'message': 'Invalid credentials'})
            
    except Exception as e:
        app.logger.error(f'Error checking moderator: {str(e)}')
        return jsonify({'status': 'error', 'message': 'Internal server error'}), 500