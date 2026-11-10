# FindIt - Lost & Found Management System

A beginner-friendly Java Swing project that stores all information in local text files. It does **not** use MySQL, SQLite, or any other database.

## Requirements
- JDK 8 or later
- VS Code, IntelliJ IDEA, or Eclipse (optional)

## Run using terminal
Open a terminal in the `FindIt_Java_Project` folder and run:

```bash
javac -encoding UTF-8 -d out src/Main.java
java -cp out Main
```

The program creates a `data` folder automatically the first time it runs.

## Demo admin account
- Username: `admin`
- Password: `admin123`

Students can create their own accounts from the login screen.

## Features
- Student registration and login
- Report lost items
- Report found items
- Browse and search open reports
- Submit claim requests
- Admin can approve/reject claims
- Admin can mark an item as returned
- Dashboard counters
- Local file storage

## Local data files
- `data/users.txt` - student accounts
- `data/items.txt` - lost/found reports
- `data/claims.txt` - claim requests

Each record is stored as a tab-separated line. Keep the `data` folder alongside the project when moving or backing up the application.

## Important notes
- This is a college demonstration project, not a production identity or security system.
- Passwords are stored as plain text for simplicity in this beginner version. Do not use real passwords.
- The default admin password should be changed before any real deployment.
- Reports and claims are stored only on the computer where the application runs; there is no online sharing.
