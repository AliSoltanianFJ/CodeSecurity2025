from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
from flask import jsonify
import re

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '')
    
    # Validate URL pattern
    if not re.fullmatch(r'^https?://[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,}$', url):
        return jsonify({'error': 'Invalid URL format'}), 400
    
    try:
        # Use subprocess to run ping command safely
        result = subprocess.run(
            ['ping', '-c', '1', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )
        
        if result.returncode == 0:
            return jsonify({'status': 'success', 'output': result.stdout}), 200
        else:
            return jsonify({'error': 'Ping failed', 'message': result.stderr}), 500
            
    except subprocess.TimeoutExpired:
        return jsonify({'error': 'Request timed out'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# Example usage when accessed without parameters
@app.route('/url/ping', methods=['GET'])
def ping_default():
    return jsonify({'error': 'URL parameter is required'}), 400