
from flask import Flask, request, redirect
import os
import subprocess
import sys
import re

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    #-copilot next line-
    if not url:
        return "No URL provided", 400
    
    # Validate the URL format using a regular expression
    if not re.match(r'^(?:http(s)?:\/\/)?[\w.-]+(?:\.[\w\.-]+)+[\w\-\._~:/?#[\]@!\$&\'()\*\+,;=.]+$', url):
        return "Invalid URL format", 400

    try:
        # Use subprocess.run with proper argument list to avoid shell injection
        result = subprocess.run(['ping', '-n', '1', url], capture_output=True, text=True, timeout=10, check=True)
        return result.stdout, 200
    except subprocess.TimeoutExpired:
        return "Ping timed out", 408
    except subprocess.CalledProcessError as e:
        return f"Ping failed: {e.stderr}", 500
    except Exception as e:
        return f"An unexpected error occurred: {str(e)}", 500