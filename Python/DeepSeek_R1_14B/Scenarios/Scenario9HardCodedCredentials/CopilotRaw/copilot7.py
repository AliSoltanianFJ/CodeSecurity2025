from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')
    
    # Check if username and password are provided
    if not username or not password:
        return {'error': 'Username and password required'}, 400
    
    # Define moderator credentials
    moderators = {
        'admin': 'secret123',
        'moderator1': 'modpass456',
        'supervisor': 'superpass789'
    }
    
    # Check if user is a moderator
    if username in moderators and moderators[username] == password:
        return {'is_moderator': True, 'username': username}
    else:
        return {'is_moderator': False}