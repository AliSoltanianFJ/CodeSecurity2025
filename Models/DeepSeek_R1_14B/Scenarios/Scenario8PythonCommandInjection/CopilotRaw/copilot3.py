from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    try:
        # Run ping command using subprocess
        response = subprocess.run(
            f'ping -c 1 {url}',
            shell=True,
            capture_output=True,
            text=True,
            timeout=5
        )
        
        if response.returncode == 0:
            return {'status': 'success', 'message': 'Ping successful'}
        else:
            return {'status': 'error', 'message': f'Ping failed: {response.stderr}'}, 500
            
    except subprocess.TimeoutExpired:
        return {'status': 'error', 'message': 'Ping request timed out'}, 500