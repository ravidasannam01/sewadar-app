import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Box,
  Chip,
  Container,
} from '@mui/material'
import { useAuth } from '../contexts/AuthContext'
import LogoutIcon from '@mui/icons-material/Logout'
import DashboardIcon from '@mui/icons-material/Dashboard'
import EventIcon from '@mui/icons-material/Event'
import AssignmentIcon from '@mui/icons-material/Assignment'
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'
import WorkflowIcon from '@mui/icons-material/AccountTree'
import PendingActionsIcon from '@mui/icons-material/PendingActions'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'

const Layout = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const navItems = [
    { path: '/programs', label: 'Programs', icon: <EventIcon /> },
    { path: '/applications', label: 'My Applications', icon: <AssignmentIcon /> },
    { path: '/dashboard', label: 'Dashboard', icon: <DashboardIcon /> },
  ]

  // INCHARGE and ADMIN get additional navigation items
  if (user?.role === 'ADMIN' || user?.role === 'INCHARGE') {
    navItems.push(
      {
        path: '/admin',
        label: 'Admin',
        icon: <AdminPanelSettingsIcon />,
      },
      {
        path: '/attendance',
        label: 'Attendance',
        icon: <CheckCircleIcon />,
      },
      {
        path: '/workflow',
        label: 'Workflow',
        icon: <WorkflowIcon />,
      }
    )
  }
  
  // SEWADAR, INCHARGE, and ADMIN can all access pending actions
  // (INCHARGE can apply to programs too, so they need this)
  if (user?.role === 'SEWADAR' || user?.role === 'INCHARGE' || user?.role === 'ADMIN') {
    navItems.push({
      path: '/pending-actions',
      label: 'Pending Actions',
      icon: <PendingActionsIcon />,
    })
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <AppBar position="sticky" sx={{ bgcolor: '#800000' }}>
        <Toolbar>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexGrow: 1 }}>
            <Box
              component="img"
              src="/img.png"
              alt="Logo"
              sx={{
                width: 40,
                height: 40,
                objectFit: 'contain',
                borderRadius: 0,
              }}
            />
            <Typography variant="h6" component="div">
              {(user?.role === 'ADMIN' || user?.role === 'INCHARGE') ? 'Incharge Management System' : 'Sewadar Management System'}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Typography variant="body2">
              {user?.firstName} {user?.lastName}
            </Typography>
            <Chip
              label={user?.role}
              size="small"
              sx={{
                bgcolor: (user?.role === 'ADMIN' || user?.role === 'INCHARGE') ? '#d4af37' : '#4a90a4',
                color: 'white',
                fontWeight: 600,
              }}
            />
            <Button
              color="inherit"
              startIcon={<LogoutIcon />}
              onClick={handleLogout}
            >
              Logout
            </Button>
          </Box>
        </Toolbar>
      </AppBar>

      <Box
        sx={{
          display: 'flex',
          bgcolor: '#F8F8F0',
          minHeight: 'calc(100vh - 64px)',
        }}
      >
        <Box
          sx={{
            width: 240,
            bgcolor: 'white',
            borderRight: '1px solid #e0e0e0',
            p: 2,
          }}
        >
          {navItems.map((item) => (
            <Button
              key={item.path}
              fullWidth
              startIcon={item.icon}
              onClick={() => navigate(item.path)}
              sx={{
                justifyContent: 'flex-start',
                mb: 1,
                bgcolor:
                  location.pathname === item.path ? 'rgba(128, 0, 0, 0.1)' : 'transparent',
                color: location.pathname === item.path ? '#800000' : 'inherit',
                '&:hover': {
                  bgcolor: 'rgba(128, 0, 0, 0.1)',
                },
              }}
            >
              {item.label}
            </Button>
          ))}
        </Box>

        <Container maxWidth="xl" sx={{ flex: 1, py: 4 }}>
          <Outlet />
        </Container>
      </Box>
    </Box>
  )
}

export default Layout

