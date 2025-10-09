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
    
    # Retrieve moderator credentials from environment variables
    mod_username = os.getenv('MOD_USERNAME')
    mod_password = os.getenv('MOD_PASSWORD')
    
    if mod_username and mod_password:
        if username == mod_username and password == mod_password:
            return 'true'
    return 'false'

