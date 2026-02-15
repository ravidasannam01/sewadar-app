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
  FormControl,
  InputLabel,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Grid,
  CircularProgress,
  Alert,
  Checkbox,
  FormControlLabel,
} from '@mui/material'
import {
  Event as EventIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material'
import { format } from 'date-fns'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const Attendance = () => {
  const { user } = useAuth()
  const [programs, setPrograms] = useState([])
  const [selectedProgram, setSelectedProgram] = useState(null)
  const [attendees, setAttendees] = useState([])
  const [attendanceRecords, setAttendanceRecords] = useState([])
  const [openMarkAttendanceDialog, setOpenMarkAttendanceDialog] = useState(false)
  const [openViewAttendanceDialog, setOpenViewAttendanceDialog] = useState(false)
  const [loading, setLoading] = useState(false)
  const [attendanceData, setAttendanceData] = useState({}) // {sewadarId: bool} - true if selected
  const [selectedDate, setSelectedDate] = useState('')
  const [attendanceNotes, setAttendanceNotes] = useState('')

  useEffect(() => {
    if (user?.role === 'INCHARGE') {
      loadPrograms()
    }
  }, [user])

  const loadPrograms = async () => {
    try {
      // For INCHARGE role, show ALL programs (not just ones they created)
      // This allows promoted incharges to see all programs in the system
      // Note: Only program creators can mark attendance (enforced by backend)
      const response = await api.get('/programs')
      // Only show active programs
      const activePrograms = response.data.filter(p => p.status === 'active')
      setPrograms(activePrograms)
    } catch (error) {
      console.error('Error loading programs:', error)
    }
  }

  const loadAttendees = async (programId) => {
    try {
      setLoading(true)
      const response = await api.get(`/attendances/program/${programId}/attendees`)
      setAttendees(response.data)
      
      // Initialize attendance data
      const initialData = {}
      response.data.forEach(attendee => {
        initialData[attendee.zonalId] = false
      })
      setAttendanceData(initialData)
      
      // Set first date as default if available
      if (selectedProgram?.programDates?.length > 0 && !selectedDate) {
        setSelectedDate(selectedProgram.programDates[0])
      }
    } catch (error) {
      console.error('Error loading attendees:', error)
      alert('Error loading attendees: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  const loadAttendanceRecords = async (programId) => {
    try {
      setLoading(true)
      const response = await api.get(`/attendances/program/${programId}`)
      setAttendanceRecords(response.data)
    } catch (error) {
      console.error('Error loading attendance records:', error)
      alert('Error loading attendance records: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  const handleSelectProgram = (program) => {
    setSelectedProgram(program)
    loadAttendees(program.id)
  }

  const handleMarkAttendance = () => {
    if (!selectedProgram) return
    setOpenMarkAttendanceDialog(true)
    loadAttendees(selectedProgram.id)
  }

  const handleViewAttendance = (program) => {
    setSelectedProgram(program)
    setOpenViewAttendanceDialog(true)
    loadAttendanceRecords(program.id)
  }

  const handleSaveAttendance = async () => {
    if (!selectedProgram || !selectedDate) {
      alert('Please select a program date')
      return
    }

    try {
      setLoading(true)
      
      // Get selected sewadar IDs
      const selectedSewadarIds = Object.entries(attendanceData)
        .filter(([sewadarId, selected]) => selected)
        .map(([sewadarId]) => parseInt(sewadarId))

      if (selectedSewadarIds.length === 0) {
        alert('Please select at least one sewadar')
        return
      }

      // Format date as YYYY-MM-DD
      const formattedDate = selectedDate

      await api.post('/attendances', {
        programId: selectedProgram.id,
        programDate: formattedDate,
        sewadarIds: selectedSewadarIds,
        notes: attendanceNotes || null
      })

      alert('Attendance marked successfully!')
      setOpenMarkAttendanceDialog(false)
      setSelectedProgram(null)
      setAttendanceData({})
      setSelectedDate('')
      setAttendanceNotes('')
      loadPrograms()
    } catch (error) {
      console.error('Error marking attendance:', error)
      alert('Error marking attendance: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }


  const updateAttendanceData = (sewadarId, selected) => {
    setAttendanceData(prev => ({
      ...prev,
      [sewadarId]: selected
    }))
  }

  // Guard: Only show for INCHARGE role
  if (!user || user.role !== 'INCHARGE') {
    return (
      <Box p={3}>
        <Alert severity="error">Access denied. This page is only available for INCHARGE users.</Alert>
      </Box>
    )
  }

  return (
    <Box>
      <Typography variant="h4" component="h1" sx={{ fontWeight: 600, mb: 3 }}>
        Attendance Management
      </Typography>

      <Grid container spacing={3}>
        {programs.map((program) => (
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
                    color={program.status === 'active' ? 'success' : 'default'}
                  />
                </Box>

                <Typography variant="body2" color="text.secondary" gutterBottom>
                  <strong>Location:</strong> {program.location} ({program.locationType})
                </Typography>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  <strong>Dates:</strong> {program.programDates?.map(d => format(new Date(d), 'MMM dd, yyyy')).join(', ') || 'N/A'}
                </Typography>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  <strong>Applications:</strong> {program.applicationCount || 0}
                  {program.maxSewadars && ` / ${program.maxSewadars}`}
                </Typography>

                <Box display="flex" gap={1} mt={2}>
                  <Button
                    variant="contained"
                    size="small"
                    startIcon={<CheckCircleIcon />}
                    onClick={() => {
                      setSelectedProgram(program)
                      handleMarkAttendance()
                    }}
                  >
                    Mark Attendance
                  </Button>
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={<EventIcon />}
                    onClick={() => handleViewAttendance(program)}
                  >
                    View Attendance
                  </Button>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {programs.length === 0 && (
        <Alert severity="info">No active programs available for attendance marking.</Alert>
      )}

      {/* Mark Attendance Dialog */}
      <Dialog
        open={openMarkAttendanceDialog}
        onClose={() => {
          setOpenMarkAttendanceDialog(false)
          setSelectedProgram(null)
          setAttendanceData({})
        }}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          Mark Attendance - {selectedProgram?.title}
        </DialogTitle>
        <DialogContent>
          {loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : attendees.length === 0 ? (
            <Alert severity="info">No approved attendees found for this program.</Alert>
          ) : (
            <Box>
              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={6}>
                  <FormControl fullWidth>
                    <InputLabel>Program Date *</InputLabel>
                    <Select
                      value={selectedDate}
                      onChange={(e) => setSelectedDate(e.target.value)}
                      label="Program Date *"
                      required
                    >
                      {selectedProgram?.programDates?.map((date) => (
                        <MenuItem key={date} value={date}>
                          {format(new Date(date), 'MMM dd, yyyy')}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Notes (Optional)"
                    multiline
                    rows={2}
                    value={attendanceNotes}
                    onChange={(e) => setAttendanceNotes(e.target.value)}
                    placeholder="Optional notes for this attendance marking"
                  />
                </Grid>
              </Grid>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Select</TableCell>
                      <TableCell>Name</TableCell>
                      <TableCell>Zonal ID</TableCell>
                      <TableCell>Mobile</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {attendees.map((attendee) => (
                      <TableRow key={attendee.zonalId}>
                        <TableCell>
                          <Checkbox
                            checked={attendanceData[attendee.zonalId] || false}
                            onChange={(e) =>
                              updateAttendanceData(attendee.zonalId, e.target.checked)
                            }
                          />
                        </TableCell>
                        <TableCell>
                          {attendee.firstName || ''} {attendee.lastName || ''}
                        </TableCell>
                        <TableCell>{attendee.zonalId || ''}</TableCell>
                        <TableCell>{attendee.mobile || '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setOpenMarkAttendanceDialog(false)
              setSelectedProgram(null)
              setAttendanceData({})
              setSelectedDate('')
              setAttendanceNotes('')
            }}
          >
            Cancel
          </Button>
          <Button
            onClick={handleSaveAttendance}
            variant="contained"
            disabled={loading || attendees.length === 0 || !selectedDate}
          >
            Save Attendance
          </Button>
        </DialogActions>
      </Dialog>

      {/* View Attendance Dialog */}
      <Dialog
        open={openViewAttendanceDialog}
        onClose={() => {
          setOpenViewAttendanceDialog(false)
          setSelectedProgram(null)
        }}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>
          Attendance Records - {selectedProgram?.title}
        </DialogTitle>
        <DialogContent>
          {loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : attendanceRecords.length === 0 ? (
            <Alert severity="info">No attendance records found for this program.</Alert>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Zonal ID</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>Notes</TableCell>
                    <TableCell>Marked At</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {attendanceRecords.map((record) => (
                    <TableRow key={record.id}>
                      <TableCell>
                        {record.sewadar?.firstName || ''} {record.sewadar?.lastName || ''}
                      </TableCell>
                      <TableCell>{record.sewadar?.zonalId || ''}</TableCell>
                      <TableCell>
                        <Chip
                          label="Present"
                          color="success"
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        {record.attendanceDate ? format(new Date(record.attendanceDate), 'MMM dd, yyyy') : '-'}
                      </TableCell>
                      <TableCell>{record.notes || '-'}</TableCell>
                      <TableCell>
                        <Typography variant="caption">
                          {record.markedAt ? format(new Date(record.markedAt), 'MMM dd, yyyy HH:mm') : '-'}
                        </Typography>
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
            setOpenViewAttendanceDialog(false)
            setSelectedProgram(null)
          }}>
            Close
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default Attendance

