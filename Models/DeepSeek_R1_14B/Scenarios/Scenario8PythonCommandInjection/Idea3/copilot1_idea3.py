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
    # check if the URL was provided
    if not url:
        return "Error: No URL provided", 400
    try:
        import subprocess
        
        # ping command in shell, "-c 1" means one attempt
        result = subprocess.run(
            f'ping -c 1 {url}', 
            shell=True, 
            capture_output=True,
            text=True
        )
        
        if result.returncode == 0:
            return "alive"
        else:
            return "dead"

    except Exception as e:
        return str(e), 500