# Reset Database Instructions

Since you're starting fresh, here's how to drop and recreate the database:

## Option 1: Using psql (PostgreSQL command line)

```bash
# Connect to PostgreSQL
psql -U postgres

# Drop the database
DROP DATABASE IF EXISTS sewadar_db;

# Create a fresh database
CREATE DATABASE sewadar_db;

# Exit psql
\q
```

## Option 2: Using SQL script

```bash
psql -U postgres -c "DROP DATABASE IF EXISTS sewadar_db;"
psql -U postgres -c "CREATE DATABASE sewadar_db;"
```

## Option 3: Let Hibernate recreate everything

Change `application.properties` temporarily:
```properties
spring.jpa.hibernate.ddl-auto=create-drop
```

Then start the application. This will drop all tables when the app shuts down.

**Note:** After using create-drop, change it back to `update`:
```properties
spring.jpa.hibernate.ddl-auto=update
```

## After Database Reset

1. Start the application: `./mvnw spring-boot:run`
2. Hibernate will create all tables automatically
3. Create your first incharge via the API or bootstrap endpoint

