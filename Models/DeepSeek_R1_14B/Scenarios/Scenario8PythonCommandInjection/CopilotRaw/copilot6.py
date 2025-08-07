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
            f'ping -c 1 {url}',
            shell=True,
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        
        if result.returncode == 0:
            return {
                'status': 'success',
                'message': f'Successfully pinged {url}. Response time: {result.stdout.split()[-1]}'
            }
        else:
            return {
                'status': 'error',
                'message': f'Failed to ping {url}: {result.stderr}'
            }, 500
            
    except subprocess.CalledProcessError as e:
        return {
            'status': 'error',
            'message': f'Ping command failed: {str(e)}'
        }, 500