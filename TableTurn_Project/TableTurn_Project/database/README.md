# XAMPP database setup

1. Open XAMPP Control Panel.
2. Start **MySQL**.
3. Open `http://localhost/phpmyadmin`.
4. Choose **Import** and select `tableturn.sql`.
5. Execute the import. The database `tableturn` and all required tables will be created.

Default connection expected by Spring Boot:
- Host: localhost
- Port: 3306
- Database: tableturn
- Username: root
- Password: empty

If your XAMPP MySQL root account has a password, edit `backend/src/main/resources/application.properties`.
