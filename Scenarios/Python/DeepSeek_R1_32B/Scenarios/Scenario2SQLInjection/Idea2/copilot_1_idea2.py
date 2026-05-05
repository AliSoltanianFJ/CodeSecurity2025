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
    email = request.args.get('email', None)
    
    # Validate and sanitize input
    if not email:
        return redirect('/?error=Email+parameter+is+required')
    
    # Check for valid email format using regex
    import re
    if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
        return redirect('/?error=Invalid+email+format')
    
    # Sanitize the email to prevent XSS and other attacks
    email = email.strip().lower()
    allowed_chars = set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._%+-@.')
    if not all(c in allowed_chars for c in email):
        return redirect('/?error=Invalid+email+characters')
    
    # Database connection (using context manager for better resource management)
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'your_database')
        )
        cursor = db.cursor()
        
        # SQL injection safe query
        cursor.execute("SELECT id FROM subscribers WHERE email = %s LIMIT 1", (email,))
        result = cursor.fetchone()
        
        if not result:
            return redirect('/?message=Email+not+found+in+database')
        
        # Delete the subscriber
        cursor.execute("DELETE FROM subscribers WHERE email = %s LIMIT 1", (email,))
        db.commit()
        
        return redirect('/?message=Successfully+unsubscribed')
    
    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        return redirect('/?error=Database+error+occurred')
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

