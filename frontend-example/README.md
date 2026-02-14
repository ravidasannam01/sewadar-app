# Frontend Example - Complete Feature Set

This is a complete HTML/JavaScript frontend with all features matching the React frontend.

## Quick Start (Recommended - No Copying Required!)

### Option 1: Use Python HTTP Server (Easiest)

1. **Start the backend** (if not already running):
   ```bash
   ./mvnw spring-boot:run
   ```

2. **In a new terminal, serve the frontend-example**:
   ```bash
   cd frontend-example
   python3 -m http.server 8000
   ```

3. **Open in browser**:
   ```
   http://localhost:8000/index.html
   ```

The frontend will automatically connect to the backend API at `http://localhost:8080/api`.

### Option 2: Use Node.js HTTP Server

```bash
cd frontend-example
npx http-server -p 8000
```

Then open: `http://localhost:8000/index.html`

### Option 3: Serve from Spring Boot (Optional)

If you prefer to serve via Spring Boot:
1. Copy files to `src/main/resources/static/`
2. Start Spring Boot: `./mvnw spring-boot:run`
3. Open: `http://localhost:8080/example.html`

## Files

- `index.html` - Main HTML file
- `app.js` - JavaScript for API calls
- `styles.css` - Basic styling

## Features

- View all Sewadars
- Create new Sewadar
- Update Sewadar
- Delete Sewadar
- View all Schedules
- Create new Schedule
- Basic error handling

## Customization

Edit `app.js` and change:
```javascript
const API_BASE_URL = 'http://localhost:8080/api';
```

For production, use your backend URL.

