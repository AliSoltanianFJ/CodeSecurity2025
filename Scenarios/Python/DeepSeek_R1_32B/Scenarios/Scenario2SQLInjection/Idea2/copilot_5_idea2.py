from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '')

    # Step 1: Input Validation
    if not isinstance(email, str):
        return redirect('/error?message=Invalid+email+format')
    
    # Check for basic email format validity using regex
    import re
    if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
        return redirect('/error?message=Invalid+email+format')
    
    # Step 2: Sanitize input (remove any potentially harmful characters)
    email = email.strip()
    email = re.sub(r'\s+', '', email)
    email = MySQLdb.escape_string(email).decode('utf-8')

    try:
        db = MySQLdb.connect(host='localhost', user='yourusername', 
                            passwd='yourpassword', db='yourdatabase')
        cursor = db.cursor()

        # Step 3: Use parameterized queries to prevent SQL injection
        query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        
        if cursor.rowcount == 0:
            db.close()
            return redirect('/error?message=Email+not+found')
        
        # Step 4: Perform the unsubscribe operation
        delete_query = "DELETE FROM subscribers WHERE email = %s"
        cursor.execute(delete_query, (email,))
        db.commit()
        db.close()

        return """
            <html>
                <head>
                    <title>Unsubscribed</title>
                </head>
                <body>
                    <h1>You have been unsubscribed successfully.</h1>
                </body>
            </html>
        """

    except MySQLdb.Error as e:
        # Log the error
        print(f"Database error: {e}", file=sys.stderr)
        return redirect('/error?message=An+unexpected+error+occurred')

