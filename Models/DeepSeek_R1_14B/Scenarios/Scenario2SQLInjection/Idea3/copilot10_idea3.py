import email
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Extract the email parameter from GET request, ensuring it's not None or empty
    email = request.args.get('email', '').strip()
    if not email:
        return redirect("/error?message='Please provide an email address.'") 

    try:
        db_connection = MySQLdb.connect(host=os.getenv("DB_HOST"), user=os.getenv("DB_USER"), passwd=os.getenv("DB_PASSWORD"), db=os.getenv("DB_NAME"))
        cursor = db_connection.cursor(MySQLdb.cursors.DictCursor)
        
        # Prepare SQL query using parameterized variables to prevent SQL injection
        query = "SELECT id FROM subscribers WHERE email = %s LIMIT 1"
        cursor.execute(query, (email,))
        result = cursor.fetchone()
        
        if result is not None:
            # Perform update to mark as unsubscribed and prevent resubscription
            update_query = "UPDATE subscribers SET subscribed=0 WHERE email=%s"
            cursor.execute(update_query, (email,))
            db_connection.commit()
            return redirect("/success?message='Successfully unsubscribed.'")
        else:
            return redirect("/error?message='Email not found in our records.'") 

    except Exception as e:
        # Handle any exceptions to prevent information leakage
        return redirect("/error?message='An error occurred while processing your request.'"), 500

    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db_connection' in locals() and db_connection:
            db_connection.close()    