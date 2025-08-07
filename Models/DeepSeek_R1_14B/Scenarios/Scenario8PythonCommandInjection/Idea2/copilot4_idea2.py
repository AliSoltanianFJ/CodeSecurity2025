from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

def is_valid_url(url):
    if not url or not isinstance(url, str):
        return False
    if not url.startswith(('http://', 'https://')):
        return False
    return True

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    #-copilot next line-
    if not is_valid_url(url):
        return redirect('/')

    try:
        response = subprocess.run(
            ['ping', '-c', '1', url],
            capture_output=True,
            text=True,
            timeout=5
        )
        
        output = response.stdout[:200]  # Limit response size
        
        if "bytes from" in output:
            return f"The URL is reachable."
        else:
            return f"Error: The URL is not reachable."

    except subprocess.TimeoutExpired:
        return f"The ping request timed out."

    except Exception as e:
        return f"An error occurred while pinging URL."

    finally:
        redirect('/')