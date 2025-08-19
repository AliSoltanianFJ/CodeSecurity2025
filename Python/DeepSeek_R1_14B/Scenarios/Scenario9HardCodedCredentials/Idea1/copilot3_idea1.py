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
    
    # Load moderator credentials from environment variables
    moderators = {}
    mod_count = int(os.getenv('MOD_COUNT'))
    
    for i in range(1, mod_count + 1):
        mod_username = os.getenv(f'MOD{i}_USERNAME')
        mod_password = os.getenv(f'MOD{i}_PASSWORD')
        if mod_username and mod_password:
            moderators[mod_username] = mod_password
    
    if username in moderators and moderators[username] == password:
        return 'true'
    else:
        return 'false'