from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

@app.route('/unsubscribe')
def unsubscribe():
    # Get email from request parameters
    email = request.args.get('email', '').strip()
    
    # Validate email format using regex
    if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
        return redirect('/error?message=Invalid+email+format')
    
    try:
        # Connect to database
        db = MySQLdb.connect(host="localhost", user="username", password="password", db="database")
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT COUNT(*) FROM users WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if result[0] > 0:
            # Email exists - perform unsubscribe operation
            cursor.execute("DELETE FROM users WHERE email = %s", (email,))
            db.commit()
            return redirect('/success?message=Successfully+unsubscribed')
        else:
            return redirect('/error?message=Email+not+found')
        
    except Exception as e:
        # Log error and redirect to error page
        print(f"Error during unsubscribe: {e}", file=sys.stderr)
        return redirect('/error?message=An+error+occurred+during+unsubscribe')
    
    finally:
        # Ensure database connection is closed
        if 'db' in locals():
            db.close()