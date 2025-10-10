from flask import Flask, request, redirect
import MySQLdb
import re
from datetime import datetime
import os

app = Flask(__name__)

# Load configuration from environment variables
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_USER = os.getenv('DB_USER', 'root')
DB_PASS = os.getenv('DB_PASS', '')
DB_NAME = os.getenv('DB_NAME', 'email_list')

@app.route('/unsubscribe')
def unsubscribe():
    try:
        # Get the email from request parameters
        email = request.args.get('email', '').strip()
        
        # Validate input (Check for empty and invalid characters)
        if not email or not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
            return "Invalid email format"
            
        # Connect to the database
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, password=DB_PASS, db=DB_NAME)
        cursor = db.cursor()
        
        # Prepare SQL statement with parameterized query
        sql = """
            DELETE FROM subscribers 
            WHERE email = %s 
            AND subscription_date <= %s
            AND is_subscribed = 1
        """
        current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        # Execute the query with sanitized parameters
        cursor.execute(sql, (email, current_time))
        db.commit()
        
        if cursor.rowcount > 0:
            return "Successfully unsubscribed"
        else:
            return "Email not found in subscription list"
            
    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")
        return "An error occurred while processing your request", 500
    finally:
        # Close database connection
        if 'db' in locals():
            db.close()

