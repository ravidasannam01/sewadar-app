# Frontend Setup & Deployment Guide

## 🎯 Frontend Options

You have several options for creating a frontend:

### Option 1: React (Recommended - Modern & Popular)
- **Location**: Create a separate `frontend` folder or separate repository
- **Best for**: Production applications, complex UIs
- **Deployment**: Can be deployed separately or served from Spring Boot

### Option 2: Static HTML/JS (Simple & Quick)
- **Location**: `src/main/resources/static/` (served by Spring Boot)
- **Best for**: Simple UIs, quick prototypes
- **Deployment**: Automatically served with Spring Boot

### Option 3: Vue.js / Angular
- **Location**: Separate project
- **Best for**: Team preferences, specific framework needs

---

## 📁 Project Structure

```
application/                    (Your Spring Boot Backend)
├── src/
│   └── main/
│       └── resources/
│           └── static/         (Option 2: Static files here)
│
frontend/                       (Option 1: Separate React project)
├── public/
├── src/
├── package.json
└── ...
```

---

## 🚀 Quick Start: React Frontend (Recommended)

### Step 1: Create React App

```bash
# Navigate to parent directory
cd /Users/ravidas.a/Desktop

# Create React app
npx create-react-app sewadar-frontend
cd sewadar-frontend

# Install additional dependencies
npm install axios
```

### Step 2: Project Structure

```
sewadar-frontend/
├── public/
├── src/
│   ├── components/
│   │   ├── SewadarList.js
│   │   ├── SewadarForm.js
│   │   ├── ScheduleList.js
│   │   └── ScheduleForm.js
│   ├── services/
│   │   └── api.js          (API service)
│   ├── App.js
│   └── index.js
└── package.json
```

### Step 3: Configure API Base URL

Create `src/services/api.js`:

```javascript
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Sewadar APIs
export const sewadarAPI = {
  getAll: () => api.get('/sewadars'),
  getById: (id) => api.get(`/sewadars/${id}`),
  create: (data) => api.post('/sewadars', data),
  update: (id, data) => api.put(`/sewadars/${id}`, data),
  delete: (id) => api.delete(`/sewadars/${id}`),
};

// Schedule APIs
export const scheduleAPI = {
  getAll: () => api.get('/schedules'),
  getById: (id) => api.get(`/schedules/${id}`),
  create: (data) => api.post('/schedules', data),
  update: (id, data) => api.put(`/schedules/${id}`, data),
  delete: (id) => api.delete(`/schedules/${id}`),
};

// Address APIs
export const addressAPI = {
  getAll: () => api.get('/addresses'),
  getById: (id) => api.get(`/addresses/${id}`),
  create: (data) => api.post('/addresses', data),
  update: (id, data) => api.put(`/addresses/${id}`, data),
  delete: (id) => api.delete(`/addresses/${id}`),
};

export default api;
```

---

## 🌐 Deployment Options

### Option A: Deploy Separately (Recommended for Production)

#### Backend Deployment:
1. **Build JAR**: `./mvnw clean package`
2. **Deploy to**: 
   - AWS EC2
   - Heroku
   - DigitalOcean
   - Your own server
3. **Domain**: `api.yourdomain.com`

#### Frontend Deployment:
1. **Build**: `npm run build`
2. **Deploy to**:
   - Netlify (Free)
   - Vercel (Free)
   - AWS S3 + CloudFront
   - GitHub Pages
3. **Domain**: `yourdomain.com` or `www.yourdomain.com`

#### Configuration:
- Frontend `.env.production`:
```
REACT_APP_API_URL=https://api.yourdomain.com/api
```

---

### Option B: Serve Frontend from Spring Boot (Simple)

1. Build React app: `npm run build`
2. Copy `build/` folder contents to `src/main/resources/static/`
3. Spring Boot will serve it at root URL
4. Deploy single JAR file

**Pros**: Single deployment, simpler
**Cons**: Frontend and backend must deploy together

---

## 📝 Deployment Steps

### 1. Backend Deployment (Spring Boot)

#### Using JAR file:
```bash
# Build
./mvnw clean package

# Run
java -jar target/application-0.0.1-SNAPSHOT.jar

# Or with profile
java -jar target/application-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
```

#### Production Properties:
Create `src/main/resources/application-production.properties`:
```properties
spring.datasource.url=jdbc:postgresql://your-db-host:5432/sewadar_db
spring.datasource.username=your_username
spring.datasource.password=your_password

server.port=8080
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
```

#### Deploy to Server:
```bash
# Copy JAR to server
scp target/application-0.0.1-SNAPSHOT.jar user@your-server:/app/

# SSH to server
ssh user@your-server

# Run with PM2 or systemd
pm2 start application-0.0.1-SNAPSHOT.jar --name sewadar-api
```

---

### 2. Frontend Deployment

#### Build for Production:
```bash
cd sewadar-frontend
npm run build
```

#### Deploy to Netlify (Easiest):
1. Install Netlify CLI: `npm install -g netlify-cli`
2. Login: `netlify login`
3. Deploy: `netlify deploy --prod --dir=build`

#### Deploy to Vercel:
```bash
npm install -g vercel
vercel --prod
```

---

### 3. Domain Configuration

#### Backend (api.yourdomain.com):
1. Point DNS A record to your server IP
2. Configure Nginx reverse proxy:
```nginx
server {
    listen 80;
    server_name api.yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

#### Frontend (yourdomain.com):
1. Point DNS to Netlify/Vercel
2. Or configure Nginx:
```nginx
server {
    listen 80;
    server_name yourdomain.com;

    root /var/www/sewadar-frontend/build;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

---

## 🔧 Environment Configuration

### Development:
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:3000`

### Production:
- Backend: `https://api.yourdomain.com`
- Frontend: `https://yourdomain.com`

---

## 📦 Quick Deployment Checklist

### Backend:
- [ ] Update `application-production.properties` with production DB
- [ ] Build JAR: `./mvnw clean package`
- [ ] Test JAR locally: `java -jar target/application-0.0.1-SNAPSHOT.jar`
- [ ] Deploy to server
- [ ] Configure domain/DNS
- [ ] Set up SSL (Let's Encrypt)
- [ ] Configure firewall (port 8080)

### Frontend:
- [ ] Set `REACT_APP_API_URL` in `.env.production`
- [ ] Build: `npm run build`
- [ ] Test build locally
- [ ] Deploy to hosting service
- [ ] Configure domain/DNS
- [ ] Test API connection

---

## 🆘 Troubleshooting

### CORS Issues:
- Already configured with `@CrossOrigin(origins = "*")`
- For production, change to specific domain: `@CrossOrigin(origins = "https://yourdomain.com")`

### API Connection Failed:
- Check backend is running
- Verify API URL in frontend
- Check CORS configuration
- Verify firewall/security groups

### Build Issues:
- Clear cache: `npm cache clean --force`
- Delete `node_modules` and reinstall
- Check Node.js version (use LTS)

---

## 📚 Next Steps

1. **Choose frontend option** (React recommended)
2. **Create frontend project** in separate directory
3. **Develop UI** using the API service structure
4. **Test locally** with backend
5. **Build for production**
6. **Deploy backend** to server
7. **Deploy frontend** to hosting
8. **Configure domains** and SSL

Would you like me to create a complete React frontend example?

