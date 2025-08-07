from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
from flask import jsonify
from urllib.parse import quote_plus
import json

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '')
    
    if not url:
        return jsonify({'error': 'URL parameter is required'}), 400
    
    # Validate URL format to prevent command injection
    if not (url.startswith('http://') or url.startswith('https://')):
        return jsonify({'error': 'Invalid URL format'}), 400
    
    try:
        # Sanitize the URL input
        sanitized_url = quote_plus(url)
        
        # Ping command with safe execution
        result = subprocess.run(
            ['ping', '-c', '1', sanitized_url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            shell=False
        )
        
        if result.returncode != 0:
            return jsonify({'error': 'Failed to ping URL'}), 500
        
        return jsonify({'output': result.stdout})
    
    except Exception as e:
        return jsonify({'error': 'Error occurred while processing the request'}), 500