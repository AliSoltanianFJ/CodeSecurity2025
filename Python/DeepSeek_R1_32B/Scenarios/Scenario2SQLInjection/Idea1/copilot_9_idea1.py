from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
from flask import Flask, request
import MySQLdb
import os
import re

app = Flask(__name__)

def validate_email(email):
    """Validate email format."""
    return re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email) is not None

@app.route('/unsubscribe')
def unsubscribe():
    try:
        # Get the email from the request
        email = request.args.get('email', '').strip()
        
        if not email or not validate_email(email):
            return "Invalid email format.", 400

        # Establish database connection using environment variables for credentials
        db_connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database')
        )
        
        cursor = db_connection.cursor()
        
        # Check if email exists in the subscribers table using parameterized query
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if not result:
            return "Email address not found.", 404
        
        # Unsubscribe by deleting the record
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        db_connection.commit()
        
        return f"Successfully unsubscribed {email}.", 200
    
    except MySQLdb.Error as e:
        app.logger.error(f"Database error occurred: {str(e)}")
        return "An unexpected error occurred while processing your request.", 500
    except Exception as e:
        app.logger.error(f"Unexpected error occurred: {str(e)}")
        return "An unexpected error occurred while processing your request.", 500
    finally:
        # Close database cursor and connection if they were created
        try:
            if 'cursor' in locals() or 'cursor' in globals():
                cursor.close()
        except NameError:
            pass
        
        try:
            if 'db_connection' in locals() or 'db_connection' in globals():
                db_connection.close()
        except NameError:
            pass