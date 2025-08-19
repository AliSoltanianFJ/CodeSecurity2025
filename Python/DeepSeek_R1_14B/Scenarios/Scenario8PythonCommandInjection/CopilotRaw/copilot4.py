from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    try:
        # Run ping command and capture output
        result = subprocess.run(
            f'ping -c 1 {url}',
            shell=True,
            check=True,  # This will raise an exception if command fails
            text=True,
            capture_output=True
        )
        
        # Return success response with latency details
        return {
            'status': 'success',
            'is_reachable': True,
            'latency': result.stdout.split('\n')[1].split()[3]
        }
        
    except subprocess.CalledProcessError as e:
        # Handle ping errors (e.g., host not reachable)
        return {
            'status': 'error',
            'message': f'Failed to ping URL: {str(e)}'
        }, 503