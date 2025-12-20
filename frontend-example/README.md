# Simple Frontend Example

This is a simple HTML/JavaScript frontend that you can use immediately.

## Quick Start

1. **Option A: Serve from Spring Boot** (Easiest)
   - Copy all files from this folder to `src/main/resources/static/`
   - Start Spring Boot: `./mvnw spring-boot:run`
   - Open: `http://localhost:8080/index.html`

2. **Option B: Serve separately**
   - Use any web server (Python, Node.js, etc.)
   - Update `API_BASE_URL` in `app.js` if needed
   - Open `index.html` in browser

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

