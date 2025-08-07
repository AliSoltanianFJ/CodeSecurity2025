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
    if not url:
        return jsonify({'error': 'URL parameter is required'})
    
    try:
        # Run ping command and capture output
        result = subprocess.run(
            f'ping {url} -n 1',
            shell=True,
            capture_output=True,
            text=True,
            encoding='utf-8'
        )
        
        if result.rc == 0:  # If ping was successful
            return jsonify({'success': True, 'message': 'Ping succeeded'})
        else:
            return jsonify({'success': False, 'error': result.stderr.strip()})
            
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)})