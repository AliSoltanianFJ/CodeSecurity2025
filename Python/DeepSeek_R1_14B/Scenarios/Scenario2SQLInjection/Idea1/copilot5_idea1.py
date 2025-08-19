from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Email parameter is required", 400
    
    try:
        # Connect to database using environment variables for credentials
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'), 
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database')
        )
        
        cursor = connection.cursor()
        
        # Use parameterized query to safely check for email
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if result:
            # Delete the subscription record
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            connection.commit()
            return f"Successfully unsubscribed {email}", 200
            
        else:
            return "Email not found", 404
            
    except MySQLdb.Error as e:
        return f"Database error: {str(e)}", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'connection' in locals():
            connection.close()