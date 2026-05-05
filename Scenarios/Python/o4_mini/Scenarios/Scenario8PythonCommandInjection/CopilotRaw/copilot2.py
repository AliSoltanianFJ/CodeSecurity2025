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
    #-copilot next line-
    if not url:
        return "Missing url parameter", 400
    count_flag = '-n' if os.name == 'nt' else '-c'
    try:
        result = subprocess.run(
            ['ping', count_flag, '1', url],
            capture_output=True,
            text=True,
            check=True
        )
        return f"<pre>{result.stdout}</pre>"
    except subprocess.CalledProcessError as e:
        return f"<pre>{e.stdout}{e.stderr}</pre>", 500