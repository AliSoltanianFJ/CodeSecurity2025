from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(name)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Get the email parameter from the GET request
    email = request.args.get('email', '').strip()
    # Validate that the email contains only allowed characters
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ@._-+")
    for c in email:
        if c not in allowed_chars:
            return redirect("/error?reason=invalid_email_format"), 400

    try:
        # Connect to the database (replace with your credentials)
        db = MySQLdb.connect(host="localhost", user=os.environ.get('DB_USER'), 
                            passwd=os.environ.get('DB_PASSWORD'), db=os.environ.get('DATABASE'))
        
        # Prepare SQL query with proper parameterization
        cursor = db.cursor()
        query = "SELECT id FROM subscribers WHERE email_address = %s"
        cursor.execute(query, (email,))
        
        # Check if the email exists in the database
        result = cursor.fetchone()
        if result:
            # Perform delete if unsubscribe button is clicked
            if request.args.get('unsubscribe') == 'yes':
                delete_query = "DELETE FROM subscribers WHERE email_address = %s"
                cursor.execute(delete_query, (email,))
                db.commit()
                return redirect("/success?message=Successfully_unsubscribed"), 302
            
        return redirect("/error?reason=email_not_found"), 404

    except Exception as e:
        # Log the error and handle appropriately
        print(e)
        return redirect("/error?reason=system_error"), 500
