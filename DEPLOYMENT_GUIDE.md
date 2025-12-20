# Complete Deployment Guide

## 🚀 Quick Start: Test Frontend Now

### Option 1: Use the Simple Frontend (Easiest - Ready Now!)

1. **Copy frontend files to Spring Boot static folder:**
```bash
cp frontend-example/*.html frontend-example/*.js frontend-example/*.css src/main/resources/static/
```

2. **Restart Spring Boot** (if running):
```bash
# Stop current instance (Ctrl+C)
# Then restart
./mvnw spring-boot:run
```

3. **Open in browser:**
```
http://localhost:8080/index.html
```

**That's it!** You now have a working frontend! 🎉

---

## 📦 Production Deployment Options

### Option A: Separate Deployment (Recommended)

#### Backend (Spring Boot API)
- **Deploy to**: AWS EC2, DigitalOcean, Heroku, Your Server
- **Domain**: `api.yourdomain.com`
- **Port**: 8080 (or configure reverse proxy)

#### Frontend (React/Static)
- **Deploy to**: Netlify, Vercel, AWS S3, GitHub Pages
- **Domain**: `yourdomain.com`
- **Configure**: Point API URL to backend

---

### Option B: Single Deployment (Simpler)

#### Serve Frontend from Spring Boot
1. Build React app: `npm run build`
2. Copy `build/` contents to `src/main/resources/static/`
3. Deploy single JAR file
4. Frontend served at root URL

**Pros**: One deployment, simpler
**Cons**: Frontend and backend deploy together

---

## 🌐 Step-by-Step Deployment

### 1. Backend Deployment

#### A. Build Production JAR
```bash
./mvnw clean package -DskipTests
```

This creates: `target/application-0.0.1-SNAPSHOT.jar`

#### B. Create Production Properties
Create `src/main/resources/application-production.properties`:
```properties
# Database
spring.datasource.url=jdbc:postgresql://your-db-host:5432/sewadar_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# Server
server.port=8080

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Logging
logging.level.root=INFO
```

#### C. Deploy to Server

**Using SSH:**
```bash
# Copy JAR to server
scp target/application-0.0.1-SNAPSHOT.jar user@your-server:/app/

# SSH to server
ssh user@your-server

# Run with Java
java -jar /app/application-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
```

**Using PM2 (Recommended for Node.js servers):**
```bash
npm install -g pm2
pm2 start application-0.0.1-SNAPSHOT.jar --name sewadar-api
pm2 save
pm2 startup
```

**Using systemd (Linux):**
Create `/etc/systemd/system/sewadar-api.service`:
```ini
[Unit]
Description=Sewadar API Service
After=network.target

[Service]
Type=simple
User=your-user
ExecStart=/usr/bin/java -jar /app/application-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
Restart=always

[Install]
WantedBy=multi-user.target
```

Then:
```bash
sudo systemctl enable sewadar-api
sudo systemctl start sewadar-api
sudo systemctl status sewadar-api
```

#### D. Configure Nginx Reverse Proxy
Create `/etc/nginx/sites-available/sewadar-api`:
```nginx
server {
    listen 80;
    server_name api.yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Enable:
```bash
sudo ln -s /etc/nginx/sites-available/sewadar-api /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

#### E. Setup SSL with Let's Encrypt
```bash
sudo apt-get install certbot python3-certbot-nginx
sudo certbot --nginx -d api.yourdomain.com
```

---

### 2. Frontend Deployment

#### A. React Frontend (If using)

**Build:**
```bash
cd sewadar-frontend
npm run build
```

**Deploy to Netlify:**
```bash
# Install Netlify CLI
npm install -g netlify-cli

# Login
netlify login

# Deploy
netlify deploy --prod --dir=build
```

**Deploy to Vercel:**
```bash
npm install -g vercel
vercel --prod
```

**Environment Variables:**
Create `.env.production`:
```
REACT_APP_API_URL=https://api.yourdomain.com/api
```

#### B. Static Frontend (Simple HTML/JS)

**Option 1: Serve from Spring Boot**
```bash
# Copy files
cp frontend-example/* src/main/resources/static/

# Update API URL in app.js
# Change: const API_BASE_URL = 'http://localhost:8080/api';
# To: const API_BASE_URL = 'https://api.yourdomain.com/api';

# Rebuild and deploy JAR
./mvnw clean package
```

**Option 2: Deploy to Netlify/Vercel**
- Upload `frontend-example/` folder
- Update `API_BASE_URL` in `app.js`
- Deploy

---

### 3. Domain Configuration

#### DNS Records

**Backend (api.yourdomain.com):**
```
Type: A
Name: api
Value: YOUR_SERVER_IP
TTL: 3600
```

**Frontend (yourdomain.com):**
- If using Netlify/Vercel: Point to their nameservers
- If using your server: Point A record to server IP

---

## 🔒 Security Checklist

- [ ] Change CORS from `*` to specific domain in production
- [ ] Use HTTPS (SSL certificate)
- [ ] Secure database credentials
- [ ] Use environment variables for secrets
- [ ] Configure firewall (only allow 80, 443, 22)
- [ ] Enable database SSL connection
- [ ] Regular backups
- [ ] Update dependencies regularly

---

## 📝 Update CORS for Production

Edit controllers to allow only your domain:

```java
@CrossOrigin(origins = "https://yourdomain.com")
```

Or create a configuration class:

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("https://yourdomain.com")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*");
            }
        };
    }
}
```

---

## 🧪 Testing Deployment

### 1. Test Backend
```bash
curl https://api.yourdomain.com/api/sewadars
```

### 2. Test Frontend
- Open: `https://yourdomain.com`
- Try creating/updating/deleting records
- Check browser console for errors

### 3. Check Logs
```bash
# PM2
pm2 logs sewadar-api

# systemd
sudo journalctl -u sewadar-api -f

# Direct
tail -f logs/application.log
```

---

## 🆘 Troubleshooting

### Backend Issues

**Port already in use:**
```bash
# Find process
lsof -i :8080
# Kill process
kill -9 <PID>
```

**Database connection failed:**
- Check database is running
- Verify credentials
- Check firewall rules
- Test connection: `psql -h host -U user -d database`

**JAR won't start:**
- Check Java version: `java -version` (need Java 21)
- Check logs: `java -jar app.jar 2>&1 | tee logs.txt`

### Frontend Issues

**CORS errors:**
- Check CORS configuration
- Verify API URL is correct
- Check browser console

**API connection failed:**
- Verify backend is running
- Check API URL in frontend
- Test API directly with curl

---

## 📊 Monitoring

### Health Check Endpoint
Spring Boot Actuator is already included. Access:
```
http://localhost:8080/actuator/health
```

### Add More Monitoring
In `application.properties`:
```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

---

## 🔄 CI/CD Pipeline (Optional)

### GitHub Actions Example

Create `.github/workflows/deploy.yml`:
```yaml
name: Deploy

on:
  push:
    branches: [ main ]

jobs:
  deploy-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Build JAR
        run: ./mvnw clean package
      - name: Deploy to server
        uses: appleboy/scp-action@master
        with:
          host: ${{ secrets.HOST }}
          username: ${{ secrets.USERNAME }}
          key: ${{ secrets.SSH_KEY }}
          source: "target/*.jar"
          target: "/app"
```

---

## 📚 Next Steps

1. ✅ Test frontend locally
2. ✅ Choose deployment option
3. ✅ Set up production database
4. ✅ Deploy backend
5. ✅ Deploy frontend
6. ✅ Configure domain
7. ✅ Setup SSL
8. ✅ Test everything
9. ✅ Monitor and maintain

---

## 💡 Quick Commands Reference

```bash
# Build
./mvnw clean package

# Run locally
./mvnw spring-boot:run

# Run with profile
java -jar app.jar --spring.profiles.active=production

# Check status (PM2)
pm2 status
pm2 logs

# Check status (systemd)
sudo systemctl status sewadar-api

# Restart
pm2 restart sewadar-api
# or
sudo systemctl restart sewadar-api
```

---

Need help? Check the logs and verify each step!

