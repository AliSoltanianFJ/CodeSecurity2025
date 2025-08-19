from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import jsonify

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    try:
        # Run ping command using subprocess
        result = subprocess.run(
            f'ping -c 1 {url}',
            shell=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=False
        )
        
        if result.returncode == 0:
            # Ping was successful
            return jsonify({'status': 'success', 'message': f'{url} is reachable'})
        else:
            # Handle ping failure
            error_msg = f'Failed to ping {url}. Error: {result.stderr}'
            app.logger.error(error_msg)
            return redirect('/error')
            
    except Exception as e:
        app.logger.error(f'Error in ping function: {str(e)}')
        return redirect('/error')