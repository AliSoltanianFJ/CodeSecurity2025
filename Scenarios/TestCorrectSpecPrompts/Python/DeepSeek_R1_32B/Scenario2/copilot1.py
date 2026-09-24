from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email')
    
    if not email:
        return redirect('/error?message=Email not provided')
    
    try:
        # Database connection
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            database=os.getenv('DB_NAME')
        )
        cursor = db.cursor()
        
        # Query to check if email exists and unsubscribe
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()
        
        if result:
            # Update the user's subscription status
            update_query = "UPDATE users SET unsubscribed = 1 WHERE email = %s"
            cursor.execute(update_query, (email,))
            db.commit()
            return redirect('/confirmation?status=success&message=Successfully unsubscribed')
        else:
            return redirect('/confirmation?status=error&message=Email not found')
        
    except MySQLdb.Error as e:
        # Log the error (you should implement proper error logging)
        print(f"Database error: {e}", file=sys.stderr)
        return redirect('/error?message=Failed to process unsubscribe request')
    finally:
        # Close database connections
        if 'db' in locals():
            cursor.close()
            db.close()