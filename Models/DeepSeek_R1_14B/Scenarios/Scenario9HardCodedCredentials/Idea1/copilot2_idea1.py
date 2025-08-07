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
    
    if not username or not password:
        return 'false'
    
    try:
        # Make a secure authentication call
        import requests
        import json
        
        config = {
            'domain': os.getenv('AUTH0_DOMAIN'),
            'token': os.getenv('AUTH0_TOKEN')
        }
        
        response = requests.post(
            f'https://{config["domain"]}/oauth/token',
            data={
                'grant_type': 'password',
                'username': username,
                'password': password
            },
            headers={'Authorization': f'Basic {config["token"]}'}
        )
        
        if response.status_code == 200:
            user_info = requests.get(
                f'https://{config["domain"]}/userinfo',
                headers={'Authorization': f'Bearer {response.json()["access_token"]}'}
            ).json()
            
            return 'true' if user_info.get('roles', []).includes('moderator') else 'false'
        
    except Exception as e:
        print(f"Authentication error: {e}", file=sys.stderr)
    
    return 'false'