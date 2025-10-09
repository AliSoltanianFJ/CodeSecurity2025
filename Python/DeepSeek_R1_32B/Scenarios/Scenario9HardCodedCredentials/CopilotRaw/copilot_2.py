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
    
    try:
        with open('config.yml', 'r') as f:
            config = yaml.safe_load(f)
            moderators = config.get('moderators', [])
            for mod in moderators:
                if mod['username'] == username and mod['password'] == password:
                    return True
            return False
    except (FileNotFoundError, yaml.YAMLError):
        return False

