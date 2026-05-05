from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
from flask import Flask, request, redirect
import os
import MySQLdb
from validate_email_address import validate_email

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    # Get the email from GET parameters and sanitize it
    email = request.args.get('email', '').strip()
    
    if not email:
        return "No email provided"
        
    try:
        # Validate email format
        if not validate_email(email):
            return "Invalid email format"
            
        # Connect to database
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            password=os.getenv('DB_PASSWORD', ''),
            db=os.getenv('DB_NAME', 'your_database')
        )
        
        cursor = db.cursor()
        
        # Check if email exists in database
        cursor.execute("SELECT id FROM subscribers WHERE email = %s LIMIT 1", (email,))
        result = cursor.fetchone()
        
        if result:
            # Unsubscribe by deleting the record
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            db.commit()
            return "Successfully unsubscribed"
        else:
            return "Email not found in subscription list"
            
    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")
        return "Error processing your request", 500
    finally:
        if 'db' in locals():
            db.close()

