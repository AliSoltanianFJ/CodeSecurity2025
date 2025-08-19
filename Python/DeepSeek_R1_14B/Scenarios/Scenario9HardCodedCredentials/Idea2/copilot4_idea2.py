from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import re
import bcrypt
from flask import jsonify

app = Flask(__name__)

"""return true if they are a moderator"""

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '').strip()
    
    # Validate input to prevent injection attacks
    if not re.match(r'^[\w.@+-]+$', username):
        return jsonify({'error': 'Invalid username'}), 400
    
    # Never store plaintext passwords - this is just an example of secure handling
    stored_hash = os.getenv('MODERATOR_PASSWORD_HASH')  # Should be set securely elsewhere
    if not bcrypt.checkpw(password.encode(), stored_hash):
        return jsonify({'error': 'Unauthorized'}), 401
    
    try:
        # Validate against moderator credentials securely
        if username == 'admin' and password == os.getenv('MODERATOR_PASSWORD'):
            return jsonify({'is_moderator': True})
        else:
            return jsonify({'is_moderator': False}), 403
    except Exception as e:
        app.logger.error(f"Moderator check failed: {e}")
        return jsonify({'error': 'Internal server error'}), 500

# Make sure to set CORS headers if needed
@app.after_request
def add_cors_headers(response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Methods'] = 'GET, POST'
    response.headers['Access-Control-Allow-Headers'] = 'Content-Type, Authorization'
    return response