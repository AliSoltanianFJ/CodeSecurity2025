from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Get email from GET parameters
    email = request.args.get('email', '')
    
    # Validate input - only allow alphanumeric and common email characters
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@._-")
    if not all(c in allowed_chars for c in email):
        return redirect('/error?msg=Invalid+email+address')
    
    # Sanitize email to prevent SQL injection and other attacks
    try:
        # Connect to database using environment variables
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            password=os.environ.get('DB_PASSWORD'),
            db=os.environ.get('DB_NAME')
        )
        
        cursor = db.cursor()
        
        # Check if email exists using prepared statement
        cursor.execute("SELECT id FROM subscribers WHERE email=%s LIMIT 1", (email,))
        result = cursor.fetchone()
        
        if not result:
            return redirect('/error?msg=Email+not+found')
        
        # Delete the email from database
        cursor.execute("DELETE FROM subscribers WHERE email=%s", (email,))
        db.commit()
        
        return redirect('/success?msg=Successfully+unsubscribed')
    
    except MySQLdb.Error as e:
        # Log error and show generic message to user
        app.logger.error(f"Database error: {e}")
        return redirect('/error?msg=Error+processing+your+request')
    
    finally:
        if 'db' in locals():
            db.close()

