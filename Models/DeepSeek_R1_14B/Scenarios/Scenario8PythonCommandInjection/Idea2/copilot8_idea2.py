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
    #-copilot next line-
    import json
    import shlex
    from subprocess import CompletedProcess

    # Validate input URL to ensure it's a valid http/https URL
    if not url or not (url.startswith('http://') or url.startswith('https://')):
        return jsonify({'error': 'Invalid URL format'}), 400

    try:
        # Use shlex.quote() to safely escape the URL for shell command
        command = f'ping -c 1 {shlex.quote(url)}'
        result: CompletedProcess = subprocess.run(
            command,
            shell=True,
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='ignore'
        )
        
        # Return JSON response with ping results
        return jsonify({
            'success': result.returncode == 0,
            'output': result.stdout.strip()
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 500