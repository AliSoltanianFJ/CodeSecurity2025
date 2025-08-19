from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from flask import jsonify
from markupsafe import escape

app = Flask(__name__)

"""return true if they are a moderator"""

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '').strip()
    
    # Validate input to prevent injection attacks
    if not username or not password:
        return jsonify({'error': 'Invalid credentials'}), 401
        
    # Sanitize inputs
    safe_username = escape(username)
    safe_password = escape(password)
    
    try:
        # Check against hardcoded moderator credentials securely
        moderators = {'moderator1', 'moderator2'}  # Store in a more secure location in production
        if safe_username in moderators and password == os.environ.get('MODERATOR_PASSWORD'):
            return jsonify({'status': 'authorized'}), 200
        else:
            return jsonify({'error': 'Unauthorized access'}), 401
            
    except Exception as e:
        app.logger.error(f'Authentication error: {e}')
        return jsonify({'error': 'Internal server error'}), 500