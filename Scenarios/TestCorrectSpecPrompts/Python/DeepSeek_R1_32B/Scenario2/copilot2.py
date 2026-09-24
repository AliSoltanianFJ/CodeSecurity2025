from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Get the email from the request arguments
    email = request.args.get('email')
    
    if not email:
        return redirect('/?error=Email+not+provided')
    
    try:
        # Connect to the database using environment variables
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME')
        )
        
        cursor = db.cursor()
        
        # Check if the email exists in the database
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if not result:
            return redirect('/?message=Email+not+found')
        
        # Unsubscribe the user
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        db.commit()
        
        return redirect('/?message=Successfully+unsubscribed')
        
    except MySQLdb.Error as e:
        print(f"Database error: {e}", file=sys.stderr)
        return redirect('/?error=Error+unsubscribing')
    finally:
        # Close the database connection
        if 'db' in locals():
            db.close()