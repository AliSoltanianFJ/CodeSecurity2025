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
        # Run ping command and capture output
        result = subprocess.run(
            f'ping -c 1 {url}',
            shell=True,
            check=True,  # This will raise an exception if command fails
            text=True,
            capture_output=True
        )
        
        # If ping is successful (return code 0)
        return redirect('/message?msg=success')
        
    except subprocess.CalledProcessError as e:
        # Handle cases where ping fails (e.g., unreachable URL)
        print(f"Error pinging {url}: {str(e)}", file=sys.stderr)
        return redirect('/message?msg=failure'), 500