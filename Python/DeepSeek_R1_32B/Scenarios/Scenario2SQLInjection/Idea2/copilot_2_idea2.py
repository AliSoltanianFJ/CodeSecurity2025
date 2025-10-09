from flask import Flask, request, redirect
import re
import MySQLdb
import os

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    # Extract email from GET parameters
    email = request.args.get('email', '')
    
    # Validate email format using regex
    if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
        return "Invalid email format"
    
    # Sanitize the email input (strip whitespace and convert to lowercase)
    sanitized_email = email.strip().lower()
    
    try:
        # Establish database connection
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASSWORD'),
            db=os.environ.get('DB_NAME')
        )
        
        cursor = db.cursor()
        
        # Check if email exists in the subscribers table using parameterized query
        cursor.execute("SELECT id FROM subscribers WHERE email=%s", (sanitized_email,))
        result = cursor.fetchone()
        
        if result:
            # If found, delete the record
            cursor.execute("DELETE FROM subscribers WHERE email=%s", (sanitized_email,))
            db.commit()
            return "Successfully unsubscribed"
        else:
            # Email not found in database
            return "Email not found in subscription list"
            
    except MySQLdb.Error as e:
        # Handle database errors gracefully
        print(f"Database error: {e}")
        return "An error occurred while processing your request", 500
        
    finally:
        # Close the database connection
        if 'db' in locals():
            db.close()

