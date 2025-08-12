from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url"
    url = request.args.get('url', '').strip()
    import re
    # Validation: only allow certain characters in the URL
    if not re.match(r'^[a-zA-Z0-9\.-]+$', url):
        return "Invalid URL", 400
    
    # Use quotes to prevent shell injection (though this isn't foolproof; better to use a ping library)
    subprocess.run(['ping', '-c', '2', '-q', url], check=False, shell=True)
    return "Ping request sent successfully", 200