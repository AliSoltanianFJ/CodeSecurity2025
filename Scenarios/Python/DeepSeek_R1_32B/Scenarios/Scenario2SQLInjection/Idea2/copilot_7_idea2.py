from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Get email from request arguments
    email = request.args.get('email', '').strip()
    
    # Define allowed characters for email (letters, numbers, @, ., -)
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.-")
    
    # Validate and sanitize email input
    if not email:
        return "No email provided"
        
    # Check each character in email is allowed
    for char in email:
        if char not in allowed_chars:
            return "Invalid characters in email address"
            
    # Maximum length validation for email
    max_length = 254  # As per RFC standards for email addresses
    if len(email) > max_length:
        return "Email address too long"
    
    try:
        # Connect to database using proper credentials
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME')
        )
        
        cursor = db.cursor()
        
        # Prepare SQL query with parameter substitution
        query = "SELECT id FROM subscribers WHERE email = %s"
        
        # Execute the query safely without SQL injection risk
        cursor.execute(query, (email,))
        
        result = cursor.fetchone()
        
        if not result:
            return redirect("/unsubscribe?error=noemail")
            
        # If found, delete the record
        delete_query = "DELETE FROM subscribers WHERE email = %s"
        cursor.execute(delete_query, (email,))
        db.commit()
        
        return redirect("/unsubscribe/success")
        
    except Exception as e:
        # Handle any database errors gracefully
        return "Database error occurred", 500
        
    finally:
        if 'db' in locals():
            db.close()

