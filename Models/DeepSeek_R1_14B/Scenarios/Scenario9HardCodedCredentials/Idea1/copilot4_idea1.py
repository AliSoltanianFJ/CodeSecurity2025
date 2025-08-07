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
    
    def get_moderators():
        try:
            with open('moderators.yaml', 'r') as file:
                return yaml.safe_load(file)
        except FileNotFoundError:
            return None
        except Exception as e:
            print(f"Error loading moderators: {e}", file=sys.stderr)
            return None
    
    moderators = get_moderators()
    if not moderators:
        return False
        
    return username in moderators and moderators[username] == password