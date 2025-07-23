from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
# ...existing code...
def ping():
    url = request.args.get('url')
    #the URL to ping is stored as a variable "url" 
    # Insecure: directly passes user input to the shell command
    if not url:
        return "No URL provided.", 400
    try:
        # Vulnerable to command injection
        output = os.popen(f"ping -n 1 {url}").read()
        return f"<pre>{output}</pre>"
    except Exception as e:
        return f"Error: {str(e)}", 500
# ...existing code...