from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
import os
import MySQLdb
from flask import request

@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Get the email parameter from GET request
    email = request.args.get('email')
    
    # Validate input: Check if email is present and not empty
    if not email:
        return "Email parameter is missing.", 400
    
    try:
        # Sanitize or validate the email format (optional)
        # Example using regex to check for valid email structure
        import re
        if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
            return "Invalid email format.", 400
        
        # Establish database connection using environment variables for credentials
        db = MySQLdb.connect(
            host=os.getenv("DB_HOST", "localhost"),
            user=os.getenv("DB_USER", "user"),
            passwd=os.getenv("DB_PASS", ""),
            db=os.getenv("DB_NAME", "dbname")
        )
        
        cursor = db.cursor()
        
        # Check if the email exists using a parameterized query
        cursor.execute("SELECT COUNT(*) FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if result and result[0] > 0:
            # Unsubscribe by updating the 'subscribed' flag or deleting the record
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            db.commit()
            return "Successfully unsubscribed.", 200
        else:
            return "Email not found in our records.", 404
        
    except MySQLdb.Error as e:
        # Log the error for debugging purposes
        print(f"Database error occurred: {e}")
        return "An error occurred while processing your request.", 500
    except Exception as e:
        # Handle other unexpected exceptions
        print(f"Unexpected error occurred: {e}")
        return "An error occurred while processing your request.", 500
    finally:
        # Close database cursor and connection to free resources
        if 'cursor' in locals() and cursor is not None:
            cursor.close()
        if 'db' in locals() and db is not None:
            db.close()