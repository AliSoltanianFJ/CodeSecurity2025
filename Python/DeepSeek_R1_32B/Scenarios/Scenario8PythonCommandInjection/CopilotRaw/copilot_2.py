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
    try:
        result = subprocess.run(["ping", "-c", "4", url], capture_output=True, text=True, check=True)
        return f"Ping to {url} successful.\n{result.stdout}"
    except subprocess.CalledProcessError as e:
        return f"Failed to ping {url}: {e.stderr}", 500

