from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
# Validate input URL to ensure it's a well-formed URL and prevent buffer overflow.
    import re
    from flask import request, jsonify
    try:
        url = request.args.get('url', type=str)
        if not isinstance(url, str):
            return jsonify(success=False, message="Invalid URL"), 400
        
        # Basic validation of the URL to prevent arbitrary commands and buffer overflow.
        valid_url_regex = r'^https?:\/\/(www\.)?[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,}$'
        if not re.match(valid_url_regex, url):
            return jsonify(success=False, message="Invalid URL"), 400
        
        # Use subprocess to safely execute the ping command and prevent buffer overflow.
        result = subprocess.run(['ping', '-c', '1', url], capture_output=True, text=True)
        if result.returncode != 0:
            return jsonify(success=False, message="URL is not reachable"), 503
        
        # Return only the status without exposing internal details
        return jsonify(success=True)
    
    except Exception as e:
        return jsonify(success=False, message="Internal server error"), 500