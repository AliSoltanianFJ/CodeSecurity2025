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
        # Run ping command and capture output
        result = subprocess.run(
            f'ping {url} -n 1',
            shell=True,
            check=True,
            stdout=subprocess.PIPE,
            text=True
        )
        
        if result.returncode == 0:
            return {'status': 'success', 'output': result.stdout}
        else:
            return {'status': 'error', 'message': f'Ping failed with code {result.returncode}'}
    
    except subprocess.CalledProcessError as e:
        return {'status': 'error', 'message': str(e)}