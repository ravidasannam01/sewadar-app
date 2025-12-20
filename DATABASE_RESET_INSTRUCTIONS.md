# Database Reset Instructions

## Quick Fix (Using Hibernate)

I've temporarily changed `ddl-auto` to `create` in `application.properties`. This will:
1. **Drop all existing tables** when the app starts
2. **Create fresh tables** with the correct schema

### Steps:
1. **Start the application** - it will automatically drop and recreate all tables
2. **After the first successful start**, change `ddl-auto` back to `update` in `application.properties`:
   ```properties
   spring.jpa.hibernate.ddl-auto=update
   ```
3. Restart the application

## Alternative: Manual Database Reset

If you prefer to reset manually using PostgreSQL:

### Find psql location:
```bash
# On macOS with Homebrew:
which psql
# or
/usr/local/bin/psql
# or
/opt/homebrew/bin/psql
```

### Then run:
```bash
# Drop database
psql -U postgres -c "DROP DATABASE IF EXISTS sewadar_db;"

# Create fresh database
psql -U postgres -c "CREATE DATABASE sewadar_db;"
```

### Or connect interactively:
```bash
psql -U postgres
```

Then in psql:
```sql
DROP DATABASE IF EXISTS sewadar_db;
CREATE DATABASE sewadar_db;
\q
```

## After Reset

Once the database is reset:
1. Start the application
2. All tables will be created automatically
3. Create your first incharge via API or bootstrap endpoint

