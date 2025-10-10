from flask import Flask, request, redirect
import os
import MySQLdb
import re

app = Flask(__name__)

# Configuration using environment variables
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USER', ''),
    'password': os.getenv('DB_PASSWORD', ''),
    'database': os.getenv('DB_NAME', '')
}

# Input validation for email format
def is_valid_email(email):
    # Check for valid email length
    if not (1 <= len(email) <= 254):
        return False
    # Email regex pattern
    email_pattern = re.compile(
        r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    )
    return bool(email_pattern.match(email))

# Sanitize input to prevent SQL injection and other attacks
def sanitize_email(email):
    # Strip whitespace and special characters
    email = email.strip()
    # Escape any special characters for MySQL
    return MySQLdb.escape_string(email)

@app.route('/unsubscribe')
def unsubscribe():
    try:
        # Get the email from GET parameters
        email = request.args.get('email', '')
        
        # Validate email format
        if not is_valid_email(email):
            app.logger.warning("Invalid email format")
            return "Invalid email format"

        # Sanitize the email
        sanitized_email = sanitize_email(email)

        # Connect to database
        db = MySQLdb.connect(**DB_CONFIG)
        cursor = db.cursor()

        # Prepare SQL query with parameterized statement
        query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(query, (sanitized_email,))
        
        # Check if email exists in the database
        result = cursor.fetchone()
        if not result:
            return "Email not found in subscription list"

        # If exists, delete from database
        delete_query = "DELETE FROM subscribers WHERE email = %s"
        cursor.execute(delete_query, (sanitized_email,))
        db.commit()

        # Close database connection
        cursor.close()
        db.close()

        return "Successfully unsubscribed"

    except MySQLdb.Error as e:
        app.logger.error("Database error")
        return "Error processing unsubscribe request"
    
    except Exception as e:
        app.logger.error("Unexpected error")
        return "An unexpected error occurred"