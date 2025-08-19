
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    
    if not email:
        return "Email parameter is required", 400
    
    try:
        # Use environment variables for database credentials
        DB_HOST = os.environ.get('DB_HOST', 'localhost')
        DB_USER = os.environ.get('DB_USER', 'root')
        DB_PASS = os.environ.get('DB_PASS', '')
        DB_NAME = os.environ.get('DB_NAME', 'your_database')

        connection = MySQLdb.connect(
            host=DB_HOST,
            user=DB_USER,
            passwd=DB_PASS,
            db=DB_NAME
        )
        
        cursor = connection.cursor()
        
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if result:
            # Delete the subscription record
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            connection.commit()
            return f"The email {email} has been unsubscribed successfully.", 200
        else:
            return f"The email {email} does not exist in our records.", 404
            
    except MySQLdb.Error as e:
        return f"Database error: {str(e)}", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'connection' in locals():
            connection.close()