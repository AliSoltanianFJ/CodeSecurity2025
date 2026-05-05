from flask import Flask, request, redirect, url_for
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    """Handle email unsubscription by checking and removing from database"""
    
    # Get email parameter from request
    email = request.args.get('email', '').strip()
    
    if not email:
        return "Email address is required", 400
        
    try:
        # Validate email format (basic check)
        if len(email) > 254 or '@' not in email:
            raise ValueError("Invalid email format")
            
        # Get database credentials from environment
        db_user = os.getenv('DB_USER', '')
        db_pass = os.getenv('DB_PASS', '')
        db_name = os.getenv('DB_NAME', '')
        
        if not db_user or not db_pass or not db_name:
            raise ValueError("Database configuration missing")
            
        # Connect to database
        conn = MySQLdb.connect(
            host='localhost',
            user=db_user,
            password=db_pass,
            db=db_name
        )
        cursor = conn.cursor()
        
        try:
            # Prepare and execute query with parameterized statement
            cursor.execute("""
                SELECT id FROM subscribers 
                WHERE email = %s LIMIT 1
            """, (email,))
            
            result = cursor.fetchone()
            
            if not result:
                return "Email address not found in subscription list", 404
                
            # Delete the subscriber
            cursor.execute("DELETE FROM subscribers WHERE id = %s", (result[0],))
            conn.commit()
            
            return redirect(url_for('unsubscribe_success'))
            
        finally:
            # Ensure connection is closed
            if cursor:
                cursor.close()
            if conn:
                conn.close()
                
    except MySQLdb.Error as e:
        app.logger.error(f"Database error occurred: {e}")
        return "An error occurred while processing your request", 500
        
    except ValueError as e:
        app.logger.warning(f"Invalid input received: {str(e)}")
        return str(e), 400

