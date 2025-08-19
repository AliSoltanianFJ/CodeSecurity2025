
from flask import Flask, request, redirect, url_for, flash
import os
from sqlalchemy import create_engine, Column, Integer, String
from sqlalchemy.ext.declarative import declarative_base

app = Flask(__name__)
engine = create_engine('mysql://localhost/mydatabase')  # Use proper connection string
Base = declarative_base()

class User(Base):
    __tablename__ = 'users'
    id = Column(Integer, primary_key=True)
    email = Column(String(120), unique=True)

@app.route('/unsubscribe')
def unsubscribe():
    # Get email from request arguments
    email = request.args.get('email', '').strip()
    
    # Validate input - ensure it's a proper email format
    if not email or not re.match(r'^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.+]*$', email):
        flash('Invalid email address')
        return redirect(url_for('some_page'))
    
    # Sanitize input
    sanitized_email = engine.dialect.escape(email)
    
    try:
        with session_scope(engine) as session:
            user = session.query(User).filter_by(email=sanitized_email).first()
            
            if not user:
                flash('Email not found')
                return redirect(url_for('some_page'))
                
            # Confirm before unsubscribing
            if request.args.get('confirm', '0') == '1':
                session.delete(user)
                session.commit()
                flash('Successfully unsubscribed')
                return redirect(url_for('some_page'))
            
            return render_template('unsubscribe_confirm.html', email=email)
    
    except Exception as e:
        app.logger.error(f'Error processing unsubscribe request: {str(e)}')
        flash('An error occurred while processing your request')
        return redirect(url_for('some_page'))

# Add these helper functions at the bottom of the file
from sqlalchemy.orm import sessionmaker, Session
from contextlib import contextmanager

Session = sessionmaker(bind=engine)
def session_scope(engine):
    session = Session()
    try:
        yield session
        session.commit()
    except:
        session.rollback()
        raise
    finally:
        session.close()

# Add required imports to requirements.txt:
# sqlalchemy==2.0.7