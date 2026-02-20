import { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Grid,
  Tabs,
  Tab,
  IconButton,
  CircularProgress,
  Alert,
  Select,
  FormControl,
  InputLabel,
  Avatar,
  Backdrop,
  Tooltip,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  PersonAdd as PersonAddIcon,
  Undo as UndoIcon,
  Visibility as VisibilityIcon,
  Person as PersonIcon,
  Event as EventIcon,
  CalendarToday as CalendarIcon,
  Close as CloseIcon,
} from '@mui/icons-material'
import { format } from 'date-fns'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import ProgramForm from '../components/ProgramForm'
import SewadarForm from '../components/SewadarForm'
import { isAdminOrIncharge } from '../utils/roleUtils'

const Admin = () => {
  const { user, loading: authLoading, refreshUser } = useAuth()
  const [activeTab, setActiveTab] = useState(0)
  const [programs, setPrograms] = useState([])
  const [sewadars, setSewadars] = useState([])
  const [applications, setApplications] = useState([])
  const [selectedProgram, setSelectedProgram] = useState(null)
  const [selectedSewadar, setSelectedSewadar] = useState(null)
  const [openProgramForm, setOpenProgramForm] = useState(false)
  const [openSewadarForm, setOpenSewadarForm] = useState(false)
  const [openApplicationsDialog, setOpenApplicationsDialog] = useState(false)
  const [openDropRequestsDialog, setOpenDropRequestsDialog] = useState(false)
  const [openPasswordDialog, setOpenPasswordDialog] = useState(false)
  const [selectedSewadarForPassword, setSelectedSewadarForPassword] = useState(null)
  const [newPassword, setNewPassword] = useState('')
  const [openRoleChangeDialog, setOpenRoleChangeDialog] = useState(false)
  const [selectedSewadarForRoleChange, setSelectedSewadarForRoleChange] = useState(null)
  const [roleChangePassword, setRoleChangePassword] = useState('')
  const [prioritizedApplications, setPrioritizedApplications] = useState([])
  const [dropRequests, setDropRequests] = useState([])
  const [loading, setLoading] = useState(false)
  const [sortBy, setSortBy] = useState('attendance')
  const [sortOrder, setSortOrder] = useState('desc')
  const [openAttendanceSummaryDialog, setOpenAttendanceSummaryDialog] = useState(false)
  const [selectedAppForAttendance, setSelectedAppForAttendance] = useState(null)
  const [attendanceSummary, setAttendanceSummary] = useState(null)
  const [selectedProgramForAttendance, setSelectedProgramForAttendance] = useState({})
  const [programAttendanceDetails, setProgramAttendanceDetails] = useState({})
  const [photoModalOpen, setPhotoModalOpen] = useState(false)
  const [selectedSewadarForPhoto, setSelectedSewadarForPhoto] = useState(null)

  useEffect(() => {
    if (isAdminOrIncharge(user)) {
      loadPrograms()
      loadSewadars()
    }
  }, [user])

  const loadPrograms = async () => {
    try {
      // For INCHARGE role, show ALL programs (not just ones they created)
      // This allows promoted incharges to see all programs in the system
      const response = await api.get('/programs')
      setPrograms(response.data || [])
    } catch (error) {
      console.error('Error loading programs:', error)
      setPrograms([])
    }
  }

  const loadSewadars = async () => {
    try {
      const response = await api.get('/sewadars')
      setSewadars(response.data || [])
    } catch (error) {
      console.error('Error loading sewadars:', error)
      setSewadars([])
    }
  }

  const loadPrioritizedApplications = async (programId) => {
    try {
      setLoading(true)
      const response = await api.get(
        `/program-applications/program/${programId}/prioritized?sortBy=${sortBy}&order=${sortOrder}`
      )
      setPrioritizedApplications(response.data)
    } catch (error) {
      console.error('Error loading applications:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadDropRequests = async (programId) => {
    try {
      setLoading(true)
      const response = await api.get(
        `/program-applications/program/${programId}/drop-requests`
      )
      setDropRequests(response.data)
    } catch (error) {
      console.error('Error loading drop requests:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleApproveApplication = async (applicationId) => {
    if (!window.confirm('Approve this application?')) return

    try {
      await api.put(`/program-applications/${applicationId}/status?status=APPROVED`)
      alert('Application approved!')
      if (selectedProgram) {
        loadPrioritizedApplications(selectedProgram.id)
      }
    } catch (error) {
      alert(error.response?.data?.message || 'Failed to approve')
    }
  }

  const handleRejectApplication = async (applicationId) => {
    if (!window.confirm('Reject this application?')) return

    try {
      await api.put(`/program-applications/${applicationId}/status?status=REJECTED`)
      alert('Application rejected.')
      if (selectedProgram) {
        loadPrioritizedApplications(selectedProgram.id)
      }
    } catch (error) {
      alert(error.response?.data?.message || 'Failed to reject')
    }
  }

  const handleRollbackApplication = async (applicationId) => {
    if (!window.confirm('Rollback this application to PENDING? This will undo the approval/rejection.')) return

    try {
      await api.put(`/program-applications/${applicationId}/rollback?inchargeId=${user.zonalId}`)
      alert('Application rolled back to PENDING.')
      if (selectedProgram) {
        loadPrioritizedApplications(selectedProgram.id)
      }
    } catch (error) {
      alert(error.response?.data?.message || 'Failed to rollback')
    }
  }

  const handleOpenAttendanceSummary = async (app) => {
    setSelectedAppForAttendance(app)
    setOpenAttendanceSummaryDialog(true)
    setAttendanceSummary(null)
    setSelectedProgramForAttendance({})
    setProgramAttendanceDetails({})
    
    // Ensure programs are loaded
    if (programs.length === 0) {
      await loadPrograms()
    }
    
    try {
      setLoading(true)
      // Fetch attendance summary - we already have the data from prioritizedApplications
      const summary = {
        totalAttendance: app.totalAttendanceCount || 0,
        beasAttendance: app.beasAttendanceCount || 0,
        nonBeasAttendance: app.nonBeasAttendanceCount || 0,
        totalDays: app.totalDaysAttended || 0,
        beasDays: app.beasDaysAttended || 0,
        nonBeasDays: app.nonBeasDaysAttended || 0,
      }
      setAttendanceSummary(summary)
    } catch (error) {
      console.error('Error loading attendance summary:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleProgramSelectForAttendance = async (fieldType, programId) => {
    if (!programId || !selectedAppForAttendance) return
    
    try {
      setLoading(true)
      // Fetch attendance records for this sewadar in this program
      const response = await api.get(`/attendances/program/${programId}`)
      const allAttendances = response.data || []
      
      // Filter for this sewadar
      const sewadarAttendances = allAttendances.filter(
        att => att.sewadar?.zonalId === selectedAppForAttendance.sewadar.zonalId
      )
      
      setSelectedProgramForAttendance(prev => ({
        ...prev,
        [fieldType]: programId
      }))
      
      setProgramAttendanceDetails(prev => ({
        ...prev,
        [fieldType]: sewadarAttendances
      }))
    } catch (error) {
      console.error('Error loading program attendance:', error)
      alert('Failed to load program attendance')
    } finally {
      setLoading(false)
    }
  }

  const handleViewSewadar = (sewadarZonalId) => {
    const sewadar = sewadars.find(s => s.zonalId === sewadarZonalId)
    if (sewadar) {
      setSelectedSewadar(sewadar)
      setOpenSewadarForm(true)
    }
  }

  const handleApproveDropRequest = async (applicationId) => {
    if (!window.confirm('Approve this drop request?')) return

    try {
      await api.put(
        `/program-applications/${applicationId}/approve-drop?inchargeId=${user.zonalId}&allowReapply=true`
      )
      alert('Drop request approved.')
      if (selectedProgram) {
        loadDropRequests(selectedProgram.id)
        loadPrograms()
      }
    } catch (error) {
      alert(error.response?.data?.message || 'Failed to approve drop request')
    }
  }

  const handleChangePassword = (sewadar) => {
    setSelectedSewadarForPassword(sewadar)
    setNewPassword('')
    setOpenPasswordDialog(true)
  }

  const handleSavePassword = async () => {
    if (!newPassword || newPassword.length < 6) {
      alert('Password must be at least 6 characters long')
      return
    }

    try {
      setLoading(true)
      // Get current sewadar data
      const sewadarResponse = await api.get(`/sewadars/${selectedSewadarForPassword.zonalId}`)
      const sewadarData = sewadarResponse.data

      // Prepare update payload with all required fields
      const updatePayload = {
        firstName: sewadarData.firstName || '',
        lastName: sewadarData.lastName || '',
        mobile: sewadarData.mobile || '',
        location: sewadarData.location || '',
        profession: sewadarData.profession || '',
        password: newPassword, // New password
        dateOfBirth: sewadarData.dateOfBirth || null,
        joiningDate: sewadarData.joiningDate || null,
        emergencyContact: sewadarData.emergencyContact || null,
        emergencyContactRelationship: sewadarData.emergencyContactRelationship || null,
        photoUrl: sewadarData.photoUrl || null,
        aadharNumber: sewadarData.aadharNumber || null,
        languages: sewadarData.languages || [],
        remarks: sewadarData.remarks || '',
        address1: sewadarData.address?.address1 || '',
        address2: sewadarData.address?.address2 || '',
        email: sewadarData.address?.email || '',
      }

      // Update with new password
      await api.put(`/sewadars/${selectedSewadarForPassword.zonalId}`, updatePayload)

      // If changing password for the current logged-in user, refresh their data
      if (selectedSewadarForPassword.zonalId === user?.zonalId && refreshUser) {
        await refreshUser()
      }

      alert('Password changed successfully!')
      setOpenPasswordDialog(false)
      setSelectedSewadarForPassword(null)
      setNewPassword('')
    } catch (error) {
      console.error('Password change error:', error)
      const errorMessage = error.response?.data?.message || error.message || 'Failed to change password'
      alert(`Error: ${errorMessage}`)
    } finally {
      setLoading(false)
    }
  }

  const handleRoleChangeClick = (sewadar) => {
    // Only allow role changes for admin/incharge
    if (!isAdminOrIncharge(user)) {
      return
    }
    
    // Don't allow changing your own role
    if (sewadar.zonalId === user.zonalId) {
      alert('You cannot change your own role')
      return
    }
    
    setSelectedSewadarForRoleChange(sewadar)
    setRoleChangePassword('')
    setOpenRoleChangeDialog(true)
  }

  const handleRoleChange = async () => {
    if (!roleChangePassword || roleChangePassword.trim().length === 0) {
      alert('Please enter your password to confirm the role change')
      return
    }

    if (!selectedSewadarForRoleChange) {
      alert('No sewadar selected')
      return
    }

    if (!user || !user.zonalId) {
      alert('User information not available')
      return
    }

    try {
      setLoading(true)
      const isCurrentlyIncharge = selectedSewadarForRoleChange.role === 'INCHARGE'
      const endpoint = isCurrentlyIncharge 
        ? `/sewadars/${selectedSewadarForRoleChange.zonalId}/demote`
        : `/sewadars/${selectedSewadarForRoleChange.zonalId}/promote`
      
      await api.post(endpoint, {}, {
        params: {
          inchargeId: user.zonalId,
          password: roleChangePassword
        }
      })

      const action = isCurrentlyIncharge ? 'demoted' : 'promoted'
      alert(`Sewadar ${action} successfully!`)
      setOpenRoleChangeDialog(false)
      setSelectedSewadarForRoleChange(null)
      setRoleChangePassword('')
      loadSewadars() // Refresh the list
    } catch (error) {
      console.error('Role change error:', error)
      const errorMessage = error.response?.data?.message || error.message || 'Failed to change role'
      alert(`Error: ${errorMessage}`)
    } finally {
      setLoading(false)
    }
  }

  const getApplicationStatusColor = (status) => {
    switch (status) {
      case 'APPROVED':
        return 'success'
      case 'REJECTED':
        return 'error'
      case 'PENDING':
        return 'warning'
      case 'DROP_REQUESTED':
        return 'info'
      case 'DROPPED':
        return 'default'
      default:
        return 'default'
    }
  }

  const handleViewApplications = (program) => {
    setSelectedProgram(program)
    setOpenApplicationsDialog(true)
    loadPrioritizedApplications(program.id)
  }

  const handleViewDropRequests = (program) => {
    setSelectedProgram(program)
    setOpenDropRequestsDialog(true)
    loadDropRequests(program.id)
  }

  // Show loading state while auth is being checked
  if (authLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  // Guard: Only show for ADMIN or INCHARGE role
  if (!isAdminOrIncharge(user)) {
    return (
      <Box p={3}>
        <Alert severity="error">Access denied. This page is only available for ADMIN or INCHARGE users.</Alert>
      </Box>
    )
  }

  return (
    <Box>
      <Typography variant="h4" component="h1" sx={{ fontWeight: 600, mb: 3 }}>
        Admin Panel
      </Typography>

      <Paper sx={{ mb: 3 }}>
        <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)}>
          <Tab label="Programs" />
          <Tab label="Sewadars" />
        </Tabs>
      </Paper>

      {activeTab === 0 && (
        <Box>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
            <Typography variant="h6">Program Management</Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setOpenProgramForm(true)}
            >
              Create Program
            </Button>
          </Box>

          <Grid container spacing={3}>
            {programs.map((program) => {
              const dropRequestsCount = program.dropRequestsCount || 0
              return (
                <Grid item xs={12} md={6} key={program.id}>
                  <Card>
                    <CardContent>
                      <Box display="flex" justifyContent="space-between" alignItems="start" mb={2}>
                        <Typography variant="h6" sx={{ fontWeight: 600 }}>
                          {program.title}
                        </Typography>
                        <Chip
                          label={program.status}
                          size="small"
                          color={
                            program.status === 'active' ? 'success' : 'default'
                          }
                        />
                      </Box>

                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        <strong>Location:</strong> {program.location}
                      </Typography>
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        <strong>Applications:</strong> {program.applicationCount || 0}
                        {program.maxSewadars && ` / ${program.maxSewadars}`}
                      </Typography>
                      {dropRequestsCount > 0 && (
                        <Typography variant="body2" sx={{ color: 'error.main' }} gutterBottom>
                          <strong>Drop Requests:</strong> {dropRequestsCount} pending
                        </Typography>
                      )}

                      <Box display="flex" gap={1} mt={2}>
                        <Button
                          variant="contained"
                          size="small"
                          onClick={() => handleViewApplications(program)}
                        >
                          View Applications
                        </Button>
                        <Button
                          variant={dropRequestsCount > 0 ? "contained" : "outlined"}
                          color={dropRequestsCount > 0 ? "error" : "primary"}
                          size="small"
                          onClick={() => handleViewDropRequests(program)}
                        >
                          Review Drop Requests
                          {dropRequestsCount > 0 && ` (${dropRequestsCount})`}
                        </Button>
                        <IconButton
                          size="small"
                          onClick={() => {
                            setSelectedProgram(program)
                            setOpenProgramForm(true)
                          }}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              )
            })}
          </Grid>

          {programs.length === 0 && (
            <Alert severity="info">No programs created yet. Create one to get started!</Alert>
          )}
        </Box>
      )}

      {activeTab === 1 && (
        <Box>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
            <Typography variant="h6">Sewadar Management</Typography>
            <Button
              variant="contained"
              startIcon={<PersonAddIcon />}
              onClick={() => setOpenSewadarForm(true)}
            >
              Add Sewadar
            </Button>
          </Box>

          <TableContainer component={Paper} sx={{ maxHeight: 520 }}>
            <Table stickyHeader size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Screener ID</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Mobile</TableCell>
                  <TableCell>Location</TableCell>
                  <TableCell>Role</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {sewadars.map((sewadar) => (
                    <TableRow key={sewadar.zonalId}>
                      <TableCell>{sewadar.zonalId}</TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center" gap={1.5}>
                          <Tooltip title="Click to view photo">
                            <Avatar
                              src={sewadar.photoUrl || undefined}
                              alt={`${sewadar.firstName || ''} ${sewadar.lastName || ''}`.trim() || 'Sewadar'}
                              onClick={() => {
                                setSelectedSewadarForPhoto(sewadar)
                                setPhotoModalOpen(true)
                              }}
                              sx={{
                                width: 30,
                                height: 30,
                                bgcolor: '#b71c1c',
                                fontSize: '0.8rem',
                                cursor: 'pointer',
                                transition: 'transform 0.2s, box-shadow 0.2s',
                                '&:hover': {
                                  transform: 'scale(1.15)',
                                  boxShadow: '0 4px 8px rgba(0,0,0,0.3)',
                                },
                              }}
                            >
                              {(sewadar.firstName?.[0] ||
                                sewadar.lastName?.[0] ||
                                sewadar.zonalId?.[0] ||
                                '?'
                              )
                                .toString()
                                .toUpperCase()}
                            </Avatar>
                          </Tooltip>
                          <Typography variant="body2">
                            {sewadar.firstName} {sewadar.lastName}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>{sewadar.mobile || ''}</TableCell>
                      <TableCell>{sewadar.location || ''}</TableCell>
                      <TableCell>
                        <Chip
                          label={sewadar.role || 'SEWADAR'}
                          size="small"
                          color={sewadar.role === 'INCHARGE' ? 'primary' : 'default'}
                          onClick={() => handleRoleChangeClick(sewadar)}
                          sx={{
                            cursor: user?.role === 'INCHARGE' && sewadar.zonalId !== user.zonalId ? 'pointer' : 'default',
                            '&:hover': user?.role === 'INCHARGE' && sewadar.zonalId !== user.zonalId ? {
                              opacity: 0.8,
                              transform: 'scale(1.05)'
                            } : {}
                          }}
                          title={user?.role === 'INCHARGE' && sewadar.zonalId !== user.zonalId 
                            ? `Click to ${sewadar.role === 'INCHARGE' ? 'demote' : 'promote'} this ${sewadar.role === 'INCHARGE' ? 'incharge' : 'sewadar'}`
                            : ''}
                        />
                      </TableCell>
                      <TableCell>
                        <Box display="flex" gap={1}>
                          <IconButton
                            size="small"
                            onClick={() => {
                              setSelectedSewadar(sewadar)
                              setOpenSewadarForm(true)
                            }}
                            title="Edit Sewadar"
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <Button
                            size="small"
                            variant="outlined"
                            color="primary"
                            onClick={() => handleChangePassword(sewadar)}
                            title="Change Password"
                          >
                            Change Password
                          </Button>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      )}

      {/* Applications Dialog */}
      <Dialog
        open={openApplicationsDialog}
        onClose={() => {
          setOpenApplicationsDialog(false)
          setSelectedProgram(null)
        }}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box component="span">
            Applications for: {selectedProgram?.title}
          </Box>
          <IconButton
            size="small"
            onClick={() => {
              setOpenApplicationsDialog(false)
              setSelectedProgram(null)
            }}
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ maxHeight: '70vh', overflowY: 'auto' }}>
          <Box display="flex" gap={2} mb={2} alignItems="flex-end">
            <Box>
              <Typography variant="caption" sx={{ mb: 0.5, display: 'block', color: 'text.secondary' }}>
                Sort By
              </Typography>
              <FormControl size="small" sx={{ minWidth: 250 }}>
                <Select
                  value={sortBy}
                  onChange={(e) => {
                    setSortBy(e.target.value)
                    if (selectedProgram) {
                      loadPrioritizedApplications(selectedProgram.id)
                    }
                  }}
                >
                  <MenuItem value="appliedAt">Applied At</MenuItem>
                  <MenuItem value="attendance">Total Attendance</MenuItem>
                  <MenuItem value="beasAttendance">BEAS Attendance</MenuItem>
                  <MenuItem value="nonBeasAttendance">Non-BEAS Attendance</MenuItem>
                  <MenuItem value="days">Total Days</MenuItem>
                  <MenuItem value="beasDays">BEAS Days</MenuItem>
                  <MenuItem value="nonBeasDays">Non-BEAS Days</MenuItem>
                </Select>
              </FormControl>
            </Box>
            <Box>
              <Typography variant="caption" sx={{ mb: 0.5, display: 'block', color: 'text.secondary' }}>
                Order By
              </Typography>
              <FormControl size="small" sx={{ minWidth: 200 }}>
                <Select
                  value={sortOrder}
                  onChange={(e) => {
                    setSortOrder(e.target.value)
                    if (selectedProgram) {
                      loadPrioritizedApplications(selectedProgram.id)
                    }
                  }}
                >
                  <MenuItem value="desc">Descending</MenuItem>
                  <MenuItem value="asc">Ascending</MenuItem>
                </Select>
              </FormControl>
            </Box>
          </Box>

          {loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer sx={{ maxHeight: '50vh' }}>
              <Table stickyHeader size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Mobile</TableCell>
                    <TableCell>Attendance Summary</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {prioritizedApplications.map((app) => (
                    <TableRow key={app.id}>
                      <TableCell>
                        <Box display="flex" alignItems="center" gap={1.5}>
                          <Tooltip title="Click to view photo">
                            <Avatar
                              src={app.sewadar.photoUrl || undefined}
                              alt={`${app.sewadar.firstName || ''} ${app.sewadar.lastName || ''}`.trim() || 'Sewadar'}
                              onClick={() => {
                                setSelectedSewadarForPhoto(app.sewadar)
                                setPhotoModalOpen(true)
                              }}
                              sx={{
                                width: 26,
                                height: 26,
                                bgcolor: '#b71c1c',
                                fontSize: '0.75rem',
                                cursor: 'pointer',
                                transition: 'transform 0.2s, box-shadow 0.2s',
                                '&:hover': {
                                  transform: 'scale(1.2)',
                                  boxShadow: '0 4px 8px rgba(0,0,0,0.3)',
                                },
                              }}
                            >
                              {(app.sewadar.firstName?.[0] ||
                                app.sewadar.lastName?.[0] ||
                                app.sewadar.zonalId?.[0] ||
                                '?'
                              )
                                .toString()
                                .toUpperCase()}
                            </Avatar>
                          </Tooltip>
                          <Typography variant="body2">
                            {app.sewadar.firstName} {app.sewadar.lastName}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>{app.sewadar.mobile}</TableCell>
                      <TableCell>
                        <IconButton
                          size="small"
                          color="primary"
                          onClick={() => handleOpenAttendanceSummary(app)}
                          title="View Attendance Summary"
                        >
                          <VisibilityIcon />
                        </IconButton>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={app.status}
                          size="small"
                          color={getApplicationStatusColor(app.status)}
                        />
                      </TableCell>
                      <TableCell>
                        <Box display="flex" gap={1}>
                          {app.status === 'PENDING' && (
                            <>
                              <IconButton
                                size="small"
                                color="success"
                                onClick={() => handleApproveApplication(app.id)}
                                title="Approve"
                              >
                                <CheckCircleIcon />
                              </IconButton>
                              <IconButton
                                size="small"
                                color="error"
                                onClick={() => handleRejectApplication(app.id)}
                                title="Reject"
                              >
                                <CancelIcon />
                              </IconButton>
                            </>
                          )}
                          {(app.status === 'APPROVED' || app.status === 'REJECTED') && (
                            <IconButton
                              size="small"
                              color="warning"
                              onClick={() => handleRollbackApplication(app.id)}
                              title="Rollback to PENDING"
                            >
                              <UndoIcon />
                            </IconButton>
                          )}
                          <IconButton
                            size="small"
                            color="info"
                            onClick={() => handleViewSewadar(app.sewadar.zonalId)}
                            title="View Sewadar"
                          >
                            <PersonIcon />
                          </IconButton>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setOpenApplicationsDialog(false)
            setSelectedProgram(null)
          }}>
            Close
          </Button>
        </DialogActions>
      </Dialog>

      {/* Drop Requests Dialog */}
      <Dialog
        open={openDropRequestsDialog}
        onClose={() => {
          setOpenDropRequestsDialog(false)
          setSelectedProgram(null)
        }}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box component="span">
            Drop Requests for: {selectedProgram?.title}
          </Box>
          <IconButton
            size="small"
            onClick={() => {
              setOpenDropRequestsDialog(false)
              setSelectedProgram(null)
            }}
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ maxHeight: '70vh', overflowY: 'auto' }}>
          {loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : dropRequests.length === 0 ? (
            <Alert severity="info">No drop requests pending.</Alert>
          ) : (
            <TableContainer sx={{ maxHeight: '50vh' }}>
              <Table stickyHeader size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Mobile</TableCell>
                    <TableCell>Applied At</TableCell>
                    <TableCell>Drop Requested</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {dropRequests.map((req) => (
                    <TableRow key={req.id}>
                      <TableCell>
                        {req.sewadar.firstName} {req.sewadar.lastName}
                      </TableCell>
                      <TableCell>{req.sewadar.mobile}</TableCell>
                      <TableCell>
                        {format(new Date(req.appliedAt), 'MMM dd, yyyy')}
                      </TableCell>
                      <TableCell>
                        {req.dropRequestedAt
                          ? format(new Date(req.dropRequestedAt), 'MMM dd, yyyy')
                          : 'N/A'}
                      </TableCell>
                      <TableCell>
                        <Button
                          variant="contained"
                          color="success"
                          size="small"
                          onClick={() => handleApproveDropRequest(req.id)}
                        >
                          Approve Drop
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setOpenDropRequestsDialog(false)
            setSelectedProgram(null)
          }}>
            Close
          </Button>
        </DialogActions>
      </Dialog>

      {/* Program Form Dialog */}
      <Dialog open={openProgramForm} onClose={() => {
        setOpenProgramForm(false)
        setSelectedProgram(null)
      }} maxWidth="md" fullWidth>
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box component="span">{selectedProgram ? 'Edit Program' : 'Create Program'}</Box>
          <IconButton
            size="small"
            onClick={() => {
              setOpenProgramForm(false)
              setSelectedProgram(null)
            }}
            aria-label="Close"
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ maxHeight: '70vh', overflowY: 'auto' }}>
          <ProgramForm
            program={selectedProgram}
            onClose={() => {
              setOpenProgramForm(false)
              setSelectedProgram(null)
            }}
            onSuccess={() => {
              setOpenProgramForm(false)
              setSelectedProgram(null)
              loadPrograms()
            }}
          />
        </DialogContent>
      </Dialog>

      {/* Sewadar Form Dialog */}
      <Dialog open={openSewadarForm} onClose={() => {
        setOpenSewadarForm(false)
        setSelectedSewadar(null)
      }} maxWidth="md" fullWidth>
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box component="span">{selectedSewadar ? 'Edit Sewadar' : 'Add Sewadar'}</Box>
          <IconButton
            size="small"
            onClick={() => {
              setOpenSewadarForm(false)
              setSelectedSewadar(null)
            }}
            aria-label="Close"
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ maxHeight: '70vh', overflowY: 'auto' }}>
          <SewadarForm
            sewadar={selectedSewadar}
            onClose={() => {
              setOpenSewadarForm(false)
              setSelectedSewadar(null)
            }}
            onSuccess={() => {
              setOpenSewadarForm(false)
              // If editing the current logged-in user, refresh their data in AuthContext
              if (selectedSewadar?.zonalId === user?.zonalId && refreshUser) {
                refreshUser()
              }
              setSelectedSewadar(null)
              loadSewadars()
            }}
          />
        </DialogContent>
      </Dialog>

      {/* Password Change Dialog */}
      <Dialog open={openPasswordDialog} onClose={() => {
        setOpenPasswordDialog(false)
        setSelectedSewadarForPassword(null)
        setNewPassword('')
      }} maxWidth="sm" fullWidth>
        <DialogTitle>
          Change Password for {selectedSewadarForPassword?.firstName} {selectedSewadarForPassword?.lastName}
        </DialogTitle>
        <DialogContent sx={{ maxHeight: '70vh', overflowY: 'auto' }}>
          <TextField
            fullWidth
            type="password"
            label="New Password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            margin="normal"
            required
            helperText="Password must be at least 6 characters long"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setOpenPasswordDialog(false)
            setSelectedSewadarForPassword(null)
            setNewPassword('')
          }}>
            Cancel
          </Button>
          <Button onClick={handleSavePassword} variant="contained" disabled={!newPassword || newPassword.length < 6}>
            Change Password
          </Button>
        </DialogActions>
      </Dialog>

      {/* Attendance Summary Dialog */}
      <Dialog
        open={openAttendanceSummaryDialog}
        onClose={() => {
          setOpenAttendanceSummaryDialog(false)
          setSelectedAppForAttendance(null)
          setAttendanceSummary(null)
          setSelectedProgramForAttendance({})
          setProgramAttendanceDetails({})
        }}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box component="span">
            Attendance Summary - {selectedAppForAttendance?.sewadar?.firstName} {selectedAppForAttendance?.sewadar?.lastName}
          </Box>
          <IconButton
            size="small"
            onClick={() => {
              setOpenAttendanceSummaryDialog(false)
              setSelectedAppForAttendance(null)
              setAttendanceSummary(null)
              setSelectedProgramForAttendance({})
              setProgramAttendanceDetails({})
            }}
            aria-label="Close"
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ maxHeight: '70vh', overflowY: 'auto' }}>
          {loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : attendanceSummary ? (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              {/* Total Attendance */}
              <Grid item xs={12}>
                <Card variant="outlined">
                  <CardContent>
                    <Box display="flex" alignItems="center" justifyContent="space-between">
                      <Box display="flex" alignItems="center" gap={1}>
                        <EventIcon color="primary" />
                        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                          Total Attendance: {attendanceSummary.totalAttendance} programs
                        </Typography>
                      </Box>
                      <FormControl size="small" sx={{ minWidth: 200 }}>
                        <Select
                          value={selectedProgramForAttendance.totalAttendance || ''}
                          onChange={(e) => handleProgramSelectForAttendance('totalAttendance', e.target.value)}
                          displayEmpty
                        >
                          <MenuItem value="">Select Program</MenuItem>
                          {programs.map((prog) => (
                            <MenuItem key={prog.id} value={prog.id}>
                              {prog.title}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Box>
                    {selectedProgramForAttendance.totalAttendance && programAttendanceDetails.totalAttendance && (
                      <Box mt={2}>
                        <Typography variant="body2" color="text.secondary">
                          Attendance Records: {programAttendanceDetails.totalAttendance.length} day(s)
                        </Typography>
                        {programAttendanceDetails.totalAttendance.map((att, idx) => (
                          <Typography key={idx} variant="caption" display="block">
                            {format(new Date(att.attendanceDate), 'MMM dd, yyyy')} - Present
                          </Typography>
                        ))}
                      </Box>
                    )}
                  </CardContent>
                </Card>
              </Grid>

              {/* BEAS Attendance */}
              <Grid item xs={12}>
                <Card variant="outlined">
                  <CardContent>
                    <Box display="flex" alignItems="center" justifyContent="space-between">
                      <Box display="flex" alignItems="center" gap={1}>
                        <EventIcon color="success" />
                        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                          BEAS Attendance: {attendanceSummary.beasAttendance} programs
                        </Typography>
                      </Box>
                      <FormControl size="small" sx={{ minWidth: 200 }}>
                        <Select
                          value={selectedProgramForAttendance.beasAttendance || ''}
                          onChange={(e) => handleProgramSelectForAttendance('beasAttendance', e.target.value)}
                          displayEmpty
                        >
                          <MenuItem value="">Select Program</MenuItem>
                          {programs.filter(p => p.locationType === 'BEAS').map((prog) => (
                            <MenuItem key={prog.id} value={prog.id}>
                              {prog.title}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Box>
                    {selectedProgramForAttendance.beasAttendance && programAttendanceDetails.beasAttendance && (
                      <Box mt={2}>
                        <Typography variant="body2" color="text.secondary">
                          Attendance Records: {programAttendanceDetails.beasAttendance.length} day(s)
                        </Typography>
                        {programAttendanceDetails.beasAttendance.map((att, idx) => (
                          <Typography key={idx} variant="caption" display="block">
                            {format(new Date(att.attendanceDate), 'MMM dd, yyyy')} - Present
                          </Typography>
                        ))}
                      </Box>
                    )}
                  </CardContent>
                </Card>
              </Grid>

              {/* Non-BEAS Attendance */}
              <Grid item xs={12}>
                <Card variant="outlined">
                  <CardContent>
                    <Box display="flex" alignItems="center" justifyContent="space-between">
                      <Box display="flex" alignItems="center" gap={1}>
                        <EventIcon color="info" />
                        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                          Non-BEAS Attendance: {attendanceSummary.nonBeasAttendance} programs
                        </Typography>
                      </Box>
                      <FormControl size="small" sx={{ minWidth: 200 }}>
                        <Select
                          value={selectedProgramForAttendance.nonBeasAttendance || ''}
                          onChange={(e) => handleProgramSelectForAttendance('nonBeasAttendance', e.target.value)}
                          displayEmpty
                        >
                          <MenuItem value="">Select Program</MenuItem>
                          {programs.filter(p => p.locationType === 'NON_BEAS').map((prog) => (
                            <MenuItem key={prog.id} value={prog.id}>
                              {prog.title}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Box>
                    {selectedProgramForAttendance.nonBeasAttendance && programAttendanceDetails.nonBeasAttendance && (
                      <Box mt={2}>
                        <Typography variant="body2" color="text.secondary">
                          Attendance Records: {programAttendanceDetails.nonBeasAttendance.length} day(s)
                        </Typography>
                        {programAttendanceDetails.nonBeasAttendance.map((att, idx) => (
                          <Typography key={idx} variant="caption" display="block">
                            {format(new Date(att.attendanceDate), 'MMM dd, yyyy')} - Present
                          </Typography>
                        ))}
                      </Box>
                    )}
                  </CardContent>
                </Card>
              </Grid>

              {/* Total Days */}
              <Grid item xs={12}>
                <Card variant="outlined">
                  <CardContent>
                    <Box display="flex" alignItems="center" justifyContent="space-between">
                      <Box display="flex" alignItems="center" gap={1}>
                        <CalendarIcon color="primary" />
                        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                          Total Days: {attendanceSummary.totalDays} days
                        </Typography>
                      </Box>
                      <FormControl size="small" sx={{ minWidth: 200 }}>
                        <Select
                          value={selectedProgramForAttendance.totalDays || ''}
                          onChange={(e) => handleProgramSelectForAttendance('totalDays', e.target.value)}
                          displayEmpty
                        >
                          <MenuItem value="">Select Program</MenuItem>
                          {programs.map((prog) => (
                            <MenuItem key={prog.id} value={prog.id}>
                              {prog.title}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Box>
                    {selectedProgramForAttendance.totalDays && programAttendanceDetails.totalDays && (
                      <Box mt={2}>
                        <Typography variant="body2" color="text.secondary">
                          Attendance Records: {programAttendanceDetails.totalDays.length} day(s)
                        </Typography>
                        {programAttendanceDetails.totalDays.map((att, idx) => (
                          <Typography key={idx} variant="caption" display="block">
                            {format(new Date(att.attendanceDate), 'MMM dd, yyyy')} - Present
                          </Typography>
                        ))}
                      </Box>
                    )}
                  </CardContent>
                </Card>
              </Grid>

              {/* BEAS Days */}
              <Grid item xs={12}>
                <Card variant="outlined">
                  <CardContent>
                    <Box display="flex" alignItems="center" justifyContent="space-between">
                      <Box display="flex" alignItems="center" gap={1}>
                        <CalendarIcon color="success" />
                        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                          BEAS Days: {attendanceSummary.beasDays} days
                        </Typography>
                      </Box>
                      <FormControl size="small" sx={{ minWidth: 200 }}>
                        <Select
                          value={selectedProgramForAttendance.beasDays || ''}
                          onChange={(e) => handleProgramSelectForAttendance('beasDays', e.target.value)}
                          displayEmpty
                        >
                          <MenuItem value="">Select Program</MenuItem>
                          {programs.filter(p => p.locationType === 'BEAS').map((prog) => (
                            <MenuItem key={prog.id} value={prog.id}>
                              {prog.title}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Box>
                    {selectedProgramForAttendance.beasDays && programAttendanceDetails.beasDays && (
                      <Box mt={2}>
                        <Typography variant="body2" color="text.secondary">
                          Attendance Records: {programAttendanceDetails.beasDays.length} day(s)
                        </Typography>
                        {programAttendanceDetails.beasDays.map((att, idx) => (
                          <Typography key={idx} variant="caption" display="block">
                            {format(new Date(att.attendanceDate), 'MMM dd, yyyy')} - Present
                          </Typography>
                        ))}
                      </Box>
                    )}
                  </CardContent>
                </Card>
              </Grid>

              {/* Non-BEAS Days */}
              <Grid item xs={12}>
                <Card variant="outlined">
                  <CardContent>
                    <Box display="flex" alignItems="center" justifyContent="space-between">
                      <Box display="flex" alignItems="center" gap={1}>
                        <CalendarIcon color="info" />
                        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                          Non-BEAS Days: {attendanceSummary.nonBeasDays} days
                        </Typography>
                      </Box>
                      <FormControl size="small" sx={{ minWidth: 200 }}>
                        <Select
                          value={selectedProgramForAttendance.nonBeasDays || ''}
                          onChange={(e) => handleProgramSelectForAttendance('nonBeasDays', e.target.value)}
                          displayEmpty
                        >
                          <MenuItem value="">Select Program</MenuItem>
                          {programs.filter(p => p.locationType === 'NON_BEAS').map((prog) => (
                            <MenuItem key={prog.id} value={prog.id}>
                              {prog.title}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Box>
                    {selectedProgramForAttendance.nonBeasDays && programAttendanceDetails.nonBeasDays && (
                      <Box mt={2}>
                        <Typography variant="body2" color="text.secondary">
                          Attendance Records: {programAttendanceDetails.nonBeasDays.length} day(s)
                        </Typography>
                        {programAttendanceDetails.nonBeasDays.map((att, idx) => (
                          <Typography key={idx} variant="caption" display="block">
                            {format(new Date(att.attendanceDate), 'MMM dd, yyyy')} - Present
                          </Typography>
                        ))}
                      </Box>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          ) : (
            <Alert severity="info">No attendance data available.</Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setOpenAttendanceSummaryDialog(false)
            setSelectedAppForAttendance(null)
            setAttendanceSummary(null)
            setSelectedProgramForAttendance({})
            setProgramAttendanceDetails({})
          }}>
            Close
          </Button>
        </DialogActions>
      </Dialog>

      {/* Role Change Dialog */}
      <Dialog 
        open={openRoleChangeDialog && !!selectedSewadarForRoleChange} 
        onClose={() => {
          setOpenRoleChangeDialog(false)
          setSelectedSewadarForRoleChange(null)
          setRoleChangePassword('')
        }} 
        maxWidth="sm" 
        fullWidth
      >
        {selectedSewadarForRoleChange && (
          <>
            <DialogTitle>
              {selectedSewadarForRoleChange.role === 'INCHARGE' ? 'Demote Incharge' : 'Promote to Incharge'}
            </DialogTitle>
            <DialogContent sx={{ maxHeight: '70vh', overflowY: 'auto' }}>
              <Typography variant="body2" sx={{ mb: 2 }}>
                You are about to {selectedSewadarForRoleChange.role === 'INCHARGE' ? 'demote' : 'promote'}{' '}
                <strong>{selectedSewadarForRoleChange.firstName} {selectedSewadarForRoleChange.lastName}</strong>{' '}
                {selectedSewadarForRoleChange.role === 'INCHARGE' ? 'from INCHARGE to SEWADAR' : 'from SEWADAR to INCHARGE'}.
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Please enter your password to confirm this action.
              </Typography>
              <TextField
                fullWidth
                type="password"
                label="Your Password"
                value={roleChangePassword || ''}
                onChange={(e) => {
                  try {
                    setRoleChangePassword(e.target.value || '')
                  } catch (error) {
                    console.error('Error updating password field:', error)
                    setRoleChangePassword('')
                  }
                }}
                margin="normal"
                required
                autoFocus
              />
            </DialogContent>
            <DialogActions>
              <Button onClick={() => {
                setOpenRoleChangeDialog(false)
                setSelectedSewadarForRoleChange(null)
                setRoleChangePassword('')
              }}>
                Cancel
              </Button>
              <Button 
                onClick={handleRoleChange} 
                variant="contained" 
                color={selectedSewadarForRoleChange.role === 'INCHARGE' ? 'error' : 'primary'}
                disabled={!roleChangePassword || roleChangePassword.trim().length === 0 || loading}
              >
                {loading ? 'Processing...' : selectedSewadarForRoleChange.role === 'INCHARGE' ? 'Demote' : 'Promote'}
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Photo Modal */}
      <Dialog
        open={photoModalOpen}
        onClose={() => {
          setPhotoModalOpen(false)
          setSelectedSewadarForPhoto(null)
        }}
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
            onClick={() => {
              setPhotoModalOpen(false)
              setSelectedSewadarForPhoto(null)
            }}
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
          
          {selectedSewadarForPhoto && (
            <Box sx={{ 
              display: 'flex', 
              flexDirection: 'column', 
              alignItems: 'center', 
              p: 4,
              pt: 5,
            }}>
              <Avatar
                src={selectedSewadarForPhoto.photoUrl || undefined}
                alt={`${selectedSewadarForPhoto.firstName || ''} ${selectedSewadarForPhoto.lastName || ''}`.trim() || 'Sewadar'}
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
                {(selectedSewadarForPhoto.firstName?.[0] ||
                  selectedSewadarForPhoto.lastName?.[0] ||
                  selectedSewadarForPhoto.zonalId?.[0] ||
                  '?'
                )
                  .toString()
                  .toUpperCase()}
              </Avatar>
              
              <Typography variant="h5" sx={{ fontWeight: 600, mb: 1, color: '#800000' }}>
                {selectedSewadarForPhoto.firstName} {selectedSewadarForPhoto.lastName}
              </Typography>
              
              <Box sx={{ display: 'flex', gap: 2, mt: 1, flexWrap: 'wrap', justifyContent: 'center' }}>
                <Chip
                  label={selectedSewadarForPhoto.zonalId || 'N/A'}
                  size="small"
                  sx={{
                    bgcolor: '#800000',
                    color: 'white',
                    fontWeight: 600,
                  }}
                />
                {selectedSewadarForPhoto.screenerCode && (
                  <Chip
                    label={selectedSewadarForPhoto.screenerCode}
                    size="small"
                    sx={{
                      bgcolor: (selectedSewadarForPhoto.role === 'ADMIN' || selectedSewadarForPhoto.role === 'INCHARGE') ? '#d4af37' : '#4a90a4',
                      color: 'white',
                      fontWeight: 600,
                    }}
                  />
                )}
                <Chip
                  label={selectedSewadarForPhoto.role || 'SEWADAR'}
                  size="small"
                  sx={{
                    bgcolor: selectedSewadarForPhoto.role === 'ADMIN' ? '#d32f2f' : selectedSewadarForPhoto.role === 'INCHARGE' ? '#1976d2' : '#757575',
                    color: 'white',
                    fontWeight: 600,
                  }}
                />
              </Box>
              
              {selectedSewadarForPhoto.location && (
                <Typography variant="body2" sx={{ mt: 2, color: 'text.secondary' }}>
                  Location: {selectedSewadarForPhoto.location}
                </Typography>
              )}
              {selectedSewadarForPhoto.mobile && (
                <Typography variant="body2" sx={{ mt: 1, color: 'text.secondary' }}>
                  Mobile: {selectedSewadarForPhoto.mobile}
                </Typography>
              )}
            </Box>
          )}
        </DialogContent>
      </Dialog>
    </Box>
  )
}

export default Admin

