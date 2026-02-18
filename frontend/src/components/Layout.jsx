import { useState } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Box,
  Chip,
  Container,
  Drawer,
  IconButton,
  useMediaQuery,
  useTheme,
  Divider,
  Tooltip,
  Avatar,
  Dialog,
  DialogContent,
  Backdrop,
} from '@mui/material'
import { useAuth } from '../contexts/AuthContext'
import LogoutIcon from '@mui/icons-material/Logout'
import DashboardIcon from '@mui/icons-material/Dashboard'
import EventIcon from '@mui/icons-material/Event'
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'
import WorkflowIcon from '@mui/icons-material/AccountTree'
import PendingActionsIcon from '@mui/icons-material/PendingActions'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import MenuIcon from '@mui/icons-material/Menu'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import CloseIcon from '@mui/icons-material/Close'

const Layout = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'))
  const isTablet = useMediaQuery(theme.breakpoints.between('sm', 'md'))
  const [mobileOpen, setMobileOpen] = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [photoModalOpen, setPhotoModalOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen)
  }

  const handleSidebarToggle = () => {
    setSidebarCollapsed(!sidebarCollapsed)
  }

  const navItems = [
    { path: '/programs', label: 'ND-Programs', icon: <EventIcon /> },
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

  const sidebarWidth = sidebarCollapsed ? 64 : 240
  const drawerWidth = 240

  const drawerContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: sidebarCollapsed ? 'center' : 'space-between',
          p: 2,
          minHeight: 64,
        }}
      >
        {!sidebarCollapsed && (
          <Typography variant="h6" sx={{ color: '#800000', fontWeight: 600 }}>
            Menu
          </Typography>
        )}
        {!isMobile && (
          <IconButton
            onClick={handleSidebarToggle}
            sx={{ color: '#800000' }}
            size="small"
          >
            <ChevronLeftIcon sx={{ transform: sidebarCollapsed ? 'rotate(180deg)' : 'none', transition: 'transform 0.3s' }} />
          </IconButton>
        )}
      </Box>
      <Divider />
      <Box sx={{ flex: 1, p: sidebarCollapsed ? 1 : 2, overflow: 'auto' }}>
        {navItems.map((item) => {
          const isActive = location.pathname === item.path
          const buttonContent = sidebarCollapsed ? (
            <Tooltip title={item.label} placement="right" arrow>
              <IconButton
                onClick={() => navigate(item.path)}
                sx={{
                  width: 48,
                  height: 48,
                  bgcolor: isActive ? 'rgba(128, 0, 0, 0.1)' : 'transparent',
                  color: isActive ? '#800000' : 'inherit',
                  '&:hover': {
                    bgcolor: 'rgba(128, 0, 0, 0.1)',
                  },
                }}
              >
                {item.icon}
              </IconButton>
            </Tooltip>
          ) : (
            <Button
              fullWidth
              startIcon={item.icon}
              onClick={() => {
                navigate(item.path)
                if (isMobile) setMobileOpen(false)
              }}
              sx={{
                justifyContent: 'flex-start',
                mb: 1,
                bgcolor: isActive ? 'rgba(128, 0, 0, 0.1)' : 'transparent',
                color: isActive ? '#800000' : 'inherit',
                '&:hover': {
                  bgcolor: 'rgba(128, 0, 0, 0.1)',
                },
              }}
            >
              {item.label}
            </Button>
          )
          return <Box key={item.path}>{buttonContent}</Box>
        })}
      </Box>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <AppBar 
        position="sticky" 
        sx={{ bgcolor: '#800000', zIndex: (theme) => theme.zIndex.drawer + 1 }}
      >
        <Toolbar>
          {isMobile && (
            <IconButton
              color="inherit"
              edge="start"
              onClick={handleDrawerToggle}
              sx={{ mr: 2 }}
            >
              <MenuIcon />
            </IconButton>
          )}
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
            <Typography 
              variant="h6" 
              component="div"
              sx={{ 
                display: { xs: isMobile ? 'none' : 'block', sm: 'block' },
                fontSize: { xs: '0.9rem', sm: '1.25rem' }
              }}
            >
              {(user?.role === 'ADMIN' || user?.role === 'INCHARGE') ? 'Incharge Management System' : 'Sewadar Management System'}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: { xs: 1, sm: 2 }, flexWrap: 'wrap' }}>
            <Tooltip title="Click to view profile photo">
              <Avatar
                src={user?.photoUrl || undefined}
                alt={`${user?.firstName || ''} ${user?.lastName || ''}`.trim() || 'User'}
                onClick={() => setPhotoModalOpen(true)}
                sx={{
                  width: 32,
                  height: 32,
                  bgcolor: '#b71c1c',
                  fontSize: '0.8rem',
                  cursor: 'pointer',
                  transition: 'transform 0.2s, box-shadow 0.2s',
                  '&:hover': {
                    transform: 'scale(1.1)',
                    boxShadow: '0 4px 8px rgba(0,0,0,0.3)',
                  },
                }}
              >
                {(user?.firstName?.[0] ||
                  user?.lastName?.[0] ||
                  user?.zonalId?.[0] ||
                  '?'
                )
                  .toString()
                  .toUpperCase()}
              </Avatar>
            </Tooltip>
            <Box sx={{ textAlign: 'right' }}>
              <Typography 
                variant="body2"
                sx={{ display: { xs: 'none', sm: 'block' } }}
              >
                {user?.firstName} {user?.lastName}
              </Typography>
              <Chip
                label={user?.screenerCode || 'NA'}
                size="small"
                sx={{
                  bgcolor: (user?.role === 'ADMIN' || user?.role === 'INCHARGE') ? '#d4af37' : '#4a90a4',
                  color: 'white',
                  fontWeight: 600,
                  fontSize: { xs: '0.7rem', sm: '0.75rem' },
                  mt: 0.2,
                }}
              />
            </Box>
            <Button
              color="inherit"
              startIcon={<LogoutIcon />}
              onClick={handleLogout}
              size={isMobile ? 'small' : 'medium'}
            >
              <Box component="span" sx={{ display: { xs: 'none', sm: 'inline' } }}>
                Logout
              </Box>
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
        {/* Mobile Drawer */}
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true, // Better open performance on mobile
          }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': {
              boxSizing: 'border-box',
              width: drawerWidth,
              bgcolor: 'white',
            },
          }}
        >
          {drawerContent}
        </Drawer>

        {/* Desktop/Tablet Sidebar */}
        <Box
          component="nav"
          sx={{
            width: { xs: 0, sm: sidebarWidth },
            flexShrink: { sm: 0 },
            transition: 'width 0.3s ease',
            bgcolor: 'white',
            borderRight: '1px solid #e0e0e0',
            display: { xs: 'none', sm: 'block' },
          }}
        >
          {drawerContent}
        </Box>

        <Box
          component="main"
          sx={{
            flexGrow: 1,
            width: { sm: `calc(100% - ${sidebarWidth}px)` },
            transition: 'width 0.3s ease',
          }}
        >
          <Container maxWidth="xl" sx={{ py: { xs: 2, sm: 4 }, px: { xs: 1, sm: 3 } }}>
            <Outlet />
          </Container>
        </Box>
      </Box>

      {/* Photo Modal */}
      <Dialog
        open={photoModalOpen}
        onClose={() => setPhotoModalOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: 3,
            overflow: 'hidden',
            background: 'linear-gradient(135deg, #f5f5f5 0%, #ffffff 100%)',
          },
        }}
        BackdropComponent={Backdrop}
        BackdropProps={{
          sx: {
            backgroundColor: 'rgba(0, 0, 0, 0.7)',
            backdropFilter: 'blur(4px)',
          },
        }}
      >
        <DialogContent sx={{ p: 0, position: 'relative' }}>
          <IconButton
            onClick={() => setPhotoModalOpen(false)}
            sx={{
              position: 'absolute',
              top: 8,
              right: 8,
              zIndex: 1,
              bgcolor: 'rgba(255, 255, 255, 0.9)',
              '&:hover': {
                bgcolor: 'rgba(255, 255, 255, 1)',
              },
            }}
          >
            <CloseIcon />
          </IconButton>
          
          <Box sx={{ 
            display: 'flex', 
            flexDirection: 'column', 
            alignItems: 'center', 
            p: 4,
            pt: 5,
          }}>
            <Avatar
              src={user?.photoUrl || undefined}
              alt={`${user?.firstName || ''} ${user?.lastName || ''}`.trim() || 'User'}
              sx={{
                width: 200,
                height: 200,
                bgcolor: '#b71c1c',
                fontSize: '4rem',
                mb: 3,
                border: '4px solid white',
                boxShadow: '0 8px 24px rgba(0,0,0,0.2)',
              }}
            >
              {(user?.firstName?.[0] ||
                user?.lastName?.[0] ||
                user?.zonalId?.[0] ||
                '?'
              )
                .toString()
                .toUpperCase()}
            </Avatar>
            
            <Typography variant="h5" sx={{ fontWeight: 600, mb: 1, color: '#800000' }}>
              {user?.firstName} {user?.lastName}
            </Typography>
            
            <Box sx={{ display: 'flex', gap: 2, mt: 1, flexWrap: 'wrap', justifyContent: 'center' }}>
              <Chip
                label={user?.zonalId || 'N/A'}
                size="small"
                sx={{
                  bgcolor: '#800000',
                  color: 'white',
                  fontWeight: 600,
                }}
              />
              <Chip
                label={user?.screenerCode || 'NA'}
                size="small"
                sx={{
                  bgcolor: (user?.role === 'ADMIN' || user?.role === 'INCHARGE') ? '#d4af37' : '#4a90a4',
                  color: 'white',
                  fontWeight: 600,
                }}
              />
              <Chip
                label={user?.role || 'SEWADAR'}
                size="small"
                sx={{
                  bgcolor: user?.role === 'ADMIN' ? '#d32f2f' : user?.role === 'INCHARGE' ? '#1976d2' : '#757575',
                  color: 'white',
                  fontWeight: 600,
                }}
              />
            </Box>
            
            {user?.location && (
              <Typography variant="body2" sx={{ mt: 2, color: 'text.secondary' }}>
                Location: {user.location}
              </Typography>
            )}
          </Box>
        </DialogContent>
      </Dialog>
    </Box>
  )
}

export default Layout

