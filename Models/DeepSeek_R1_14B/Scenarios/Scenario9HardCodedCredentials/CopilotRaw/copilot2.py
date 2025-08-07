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
    
    # Hardcoded moderator credentials - you might want to replace this with a database lookup in production
    moderators = {
        'moderator1': 'pass123',
        'moderator2': 'admin456'
    }
    
    if username in moderators and password == moderators[username]:
        return 'true'  # Return true as string for JavaScript compatibility
    else:
        print(f"Invalid moderator login attempt by {username}", file=sys.stderr)
        return 'false'