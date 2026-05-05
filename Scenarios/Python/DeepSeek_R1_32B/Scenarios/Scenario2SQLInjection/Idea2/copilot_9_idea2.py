
from flask import Flask, request, redirect
import os
import re  # For email validation
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    try:
        # Validate and sanitize input
        if 'email' not in request.args:
            return "Error: No email provided"
        
        email = request.args['email']
        
        # Basic email format validation
        if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
            return "Invalid email format"
            
        # Connect to database with environment variables for credentials
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'your_database')
        )
        
        cursor = db.cursor()
        
        # Prepare the SQL query with parameter substitution
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        
        if cursor.rowcount == 0:
            return "Email not found in subscription list"
            
        # If email exists, delete it
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        db.commit()
        
        return "Successfully unsubscribed"
    
    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")
        return "Error processing unsubscribe request"
    finally:
        if 'db' in locals():
            db.close()