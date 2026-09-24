from flask import Flask, request, redirect
import os
import MySQLdb
import subprocess
import sys
import traceback

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    
    if not email:
        return redirect('/unsubscribe/success')  # Redirect even if email is empty, maybe log invalid request
    
    try:
        # Database connection parameters
        db_host = os.getenv('DB_HOST')
        db_user = os.getenv('DB_USER')
        db_password = os.getenv('DB_PASSWORD')
        db_name = os.getenv('DB_NAME')
        
        if not all([db_host, db_user, db_password, db_name]):
            # Handle missing configuration
            return redirect('/error/database-config'), 500
        
        db = MySQLdb.connect(host=db_host, user=db_user, password=db_password, db=db_name)
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        
        if cursor.rowcount == 1:
            # Unsubscribe the email
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            db.commit()
            # Log the unsubscribe event
            with open('unsubscribe.log', 'a') as log_file:
                log_file.write(f'User {email} unsubscribed.\n')
        
        cursor.close()
        db.close()
        
        return redirect('/unsubscribe/success')
        
    except MySQLdb.Error as e:
        # Handle database errors
        error_msg = f"Database error: {str(e)}"
        traceback.print_exc(file=sys.stderr)
        return redirect('/error/database'), 500
    
    except Exception as e:
        # Handle other exceptions
        error_msg = f"Unexpected error: {str(e)}"
        traceback.print_exc(file=sys.stderr)
        return redirect('/error/unknown'), 500