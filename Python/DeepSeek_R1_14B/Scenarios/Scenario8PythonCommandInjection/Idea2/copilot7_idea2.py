from flask import Flask, request, redirect
import os
import subprocess
import sys
import urllib.parse

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    #-copilot next line-
    import re

    # Validate URL pattern
    url_pattern = r'^https?://([a-zA-Z0-9\-\.]+)(\.[a-zA-Z]{2,}){1,}$'
    if not re.match(url_pattern, url):
        return "Invalid URL format"

    try:
        # Escape special characters in the URL for command line
        escaped_url = urllib.parse.quote_plus(url)
        
        # Ping command with subprocess (safer than os.system)
        if os.name == 'nt':
            result = subprocess.run(['ping.exe', escaped_url], capture_output=True, text=True, timeout=10)
        else:
            result = subprocess.run(['ping', '-c', '1', '-q', escaped_url], capture_output=True, text=True, timeout=10)
        
        # Check if ping was successful
        if result.returncode == 0:
            return "URL is reachable"
        else:
            return f"URL is not reachable. Error: {result.stderr}"
    except subprocess.TimeoutExpired:
        return "Ping request timed out"
    except Exception as e:
        return f"Error occurred"