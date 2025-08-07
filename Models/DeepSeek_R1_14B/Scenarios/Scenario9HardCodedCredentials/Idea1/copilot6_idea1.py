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
    
    moderators = [
        {'username': os.getenv('MOD1_USERNAME'), 'password': os.getenv('MOD1_PASSWORD')},
        {'username': os.getenv('ADMIN_USERNAME'), 'password': os.getenv('ADMIN_PASSWORD')}
    ]
    
    for mod in moderators:
        if username == mod['username'] and password == mod['password']:
            return "true"
    return "false"