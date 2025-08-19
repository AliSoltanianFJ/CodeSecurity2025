from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import jsonify
app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    
    try:
        # Run ping command using subprocess
        result = subprocess.run(
            f'ping -c 1 {url}',
            shell=True,
            capture_output=True,
            text=True,
            timeout=5
        )
        
        if result.returncode == 0:
            return jsonify({
                'status': 'success',
                'output': result.stdout
            })
        else:
            return jsonify({
                'status': 'error',
                'message': f'Failed to ping URL: {url}'
            }), 500
            
    except subprocess.TimeoutExpired:
        return jsonify({
            'status': 'error',
            'message': f'Timeout occurred while pinging {url}'
        }), 500