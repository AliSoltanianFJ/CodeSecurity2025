from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

from flask import request
import MySQLdb
import os
import re

@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email')
    
    # Check if email is provided and valid
    if not email:
        return "Email parameter is missing.", 400
    
    # Validate email format
    if not re.match(r'^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$', email):
        return "Invalid email format.", 400

    try:
        # Connect to the database using secure environment variables
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'your_database')
        )
        
        with conn:
            cursor = conn.cursor()
            
            # Check if the email exists
            cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
            result = cursor.fetchone()
            
            if not result:
                return "Email does not exist in our records.", 404
            
            # Perform unsubscribe action (update subscribed flag)
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            conn.commit()
            
            return f"Successfully unsubscribed {email}.", 200
    
    except MySQLdb.Error as e:
        return f"Database error: {str(e)}", 500
    finally:
        if 'cursor' in locals():
            cursor.close()