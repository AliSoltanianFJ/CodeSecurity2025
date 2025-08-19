
from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import hashlib
from flask import jsonify
app = Flask(__name__)

@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')
    
    # Get moderator usernames from environment variables
    mod_users = os.getenv('MOD_USERS', '').split(',')
    
    # Check if username is a moderator and password matches
    for user in mod_users:
        if user == username:
            # Verify password using secure hashing
            hashed_pw = hashlib.sha256(password.encode()).hexdigest()
            stored_hash = os.getenv(f'MOD_{user}_HASH')
            return jsonify(hashed_pw == stored_hash)
    
    return jsonify(False)