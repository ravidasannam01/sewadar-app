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
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  PersonAdd as PersonAddIcon,
  Undo as UndoIcon,
} from '@mui/icons-material'
import { format } from 'date-fns'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import ProgramForm from '../components/ProgramForm'
import SewadarForm from '../components/SewadarForm'
import { isAdminOrIncharge } from '../utils/roleUtils'

const Admin = () => {
  const { user, loading: authLoading } = useAuth()
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

          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Zonal ID</TableCell>
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
                        {sewadar.firstName} {sewadar.lastName}
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
        <DialogTitle>
          Applications for: {selectedProgram?.title}
        </DialogTitle>
        <DialogContent>
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
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Mobile</TableCell>
                    <TableCell>Total Attendance</TableCell>
                    <TableCell>Total Days</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {prioritizedApplications.map((app) => (
                    <TableRow key={app.id}>
                      <TableCell>
                        {app.sewadar.firstName} {app.sewadar.lastName}
                      </TableCell>
                      <TableCell>{app.sewadar.mobile}</TableCell>
                      <TableCell>{app.totalAttendanceCount || 0}</TableCell>
                      <TableCell>{app.totalDaysAttended || 0}</TableCell>
                      <TableCell>
                        <Chip label={app.status} size="small" />
                      </TableCell>
                      <TableCell>
                        {app.status === 'PENDING' && (
                          <Box display="flex" gap={1}>
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
                          </Box>
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
        <DialogTitle>
          Drop Requests for: {selectedProgram?.title}
        </DialogTitle>
        <DialogContent>
          {loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : dropRequests.length === 0 ? (
            <Alert severity="info">No drop requests pending.</Alert>
          ) : (
            <TableContainer>
              <Table>
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
        <DialogTitle>
          {selectedProgram ? 'Edit Program' : 'Create Program'}
        </DialogTitle>
        <DialogContent>
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
        <DialogTitle>
          {selectedSewadar ? 'Edit Sewadar' : 'Add Sewadar'}
        </DialogTitle>
        <DialogContent>
          <SewadarForm
            sewadar={selectedSewadar}
            onClose={() => {
              setOpenSewadarForm(false)
              setSelectedSewadar(null)
            }}
            onSuccess={() => {
              setOpenSewadarForm(false)
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
        <DialogContent>
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
            <DialogContent>
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
    </Box>
  )
}

export default Admin

