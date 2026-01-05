# Sewadar Management System - React Frontend

Modern, production-ready React frontend for the Sewadar Management System.

## Features

- ✅ Modern React with Vite
- ✅ Material-UI (MUI) for enterprise-grade components
- ✅ Pagination and search for all lists
- ✅ Responsive design
- ✅ JWT authentication
- ✅ Role-based access control
- ✅ Clean, minimal UI with no redundant elements

## Getting Started

### Prerequisites

- Node.js 18+ and npm/yarn
- Backend API running on http://localhost:8080

### Installation

```bash
cd frontend
npm install
```

### Development

```bash
npm run dev
```

The app will be available at http://localhost:3000

### Build for Production

```bash
npm run build
```

This will build the app and output to `../src/main/resources/static` for Spring Boot to serve.

## Project Structure

```
frontend/
├── src/
│   ├── components/      # Reusable components
│   ├── contexts/        # React contexts (Auth)
│   ├── pages/          # Page components
│   ├── services/       # API services
│   ├── App.jsx         # Main app component
│   └── main.jsx        # Entry point
├── index.html
├── package.json
└── vite.config.js
```

## Key Improvements

1. **Pagination**: All lists (Programs, Sewadars, Applications) now have pagination
2. **Search**: Real-time search functionality
3. **Filters**: Advanced filtering options
4. **Modern UI**: Material-UI components for professional look
5. **No Redundancy**: Removed unnecessary UI elements
6. **Better UX**: Loading states, error handling, responsive design

## API Integration

The frontend connects to the Spring Boot backend API at `/api`. Make sure CORS is properly configured in the backend.

## Deployment

1. Build the frontend: `npm run build`
2. The built files will be in `src/main/resources/static`
3. Spring Boot will serve these files automatically
4. Access the app at your backend URL (e.g., http://localhost:8080)

