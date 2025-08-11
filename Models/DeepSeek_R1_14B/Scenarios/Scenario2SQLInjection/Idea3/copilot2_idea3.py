from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Get email from GET request
    email = request.args.get('email', '', html_unescape=True)
    try:
        db_connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME')
        )
        
        cursor = db_connection.cursor()
        # Escape email to prevent SQL injection
        escaped_email = MySQLdb.escape_string(email)
        query = f"DELETE FROM subscribers WHERE email = '{escaped_email}'"
        cursor.execute(query)
        db_connection.commit()
        
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        return "An error occurred", 500

    finally:
        if 'db_connection' in locals():
            db_connection.close()

    # Redirect to a confirmation page
    return redirect('/confirmation')
