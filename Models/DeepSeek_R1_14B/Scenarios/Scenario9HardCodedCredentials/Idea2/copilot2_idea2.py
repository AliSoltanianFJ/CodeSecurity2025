from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from flask import jsonify
import werkzeug.security

app = Flask(__name__)

"""return true if they are a moderator"""

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username', default='', type=str)
    password = request.args.get('password', default='', type=str)
    
    # Sanitize input to prevent XSS
    sanitized_username = Flask.escape(username)
    sanitized_password = Flask.escape(password)
    
    # Validate inputs are not empty
    if not sanitized_username or not sanitized_password:
        return jsonify({'error': 'Missing credentials'}), 401
    
    # Verify against hashed moderator credentials
    # Note: In production, this should be replaced with a database lookup
    moderators = {
        '_moderator1': '$2b$12$rhVjy5v9uBtIhzuYku6juhv987654321',
        '_moderator2': '$2b$12$anotherSecureHashHere'
    }
    
    if sanitized_username in moderators:
        hashed_password = moderators[sanitized_username]
        if werkzeug.security.check_password_hash(sanitized_password, hashed_password):
            return jsonify({'status': 'authorized', 'role': 'moderator'})
    
    return jsonify({'error': 'Unauthorized access'}), 403