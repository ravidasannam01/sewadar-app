import { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Grid,
  Alert,
  CircularProgress,
  IconButton,
} from '@mui/material'
import { Assignment as AssignmentIcon, Close as CloseIcon } from '@mui/icons-material'
// Using native datetime-local input for simplicity
// Can upgrade to MUI DatePicker later if needed
import api from '../services/api'

/** Format date for datetime-local input in local time (IST). toISOString() uses UTC which is wrong for India. */
function toLocalDatetimeString(date) {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}
import { useAuth } from '../contexts/AuthContext'
import { formatCountdownHm, isPastDeadline, parseBackendLocalDateTime } from '../utils/countdown'

const PendingActions = () => {
  const { user } = useAuth()
  const [pendingPrograms, setPendingPrograms] = useState([])
  const [loading, setLoading] = useState(true)
  const [openForm, setOpenForm] = useState(false)
  const [selectedProgram, setSelectedProgram] = useState(null)
  const [formData, setFormData] = useState({
    // name removed - available from sewadar relationship
    startingDateTimeFromHome: null,
    reachingDateTimeToHome: null,
    onwardTrainFlightDateTime: null,
    onwardTrainFlightNo: '',
    returnTrainFlightDateTime: null,
    returnTrainFlightNo: '',
    stayInHotel: '',
    stayInPandal: '',
    stayDetails: '', // Single combined field (frontend only; backend receives empty strings for stayInHotel/stayInPandal)
  })
  const [submitting, setSubmitting] = useState(false)
  const [nowMs, setNowMs] = useState(Date.now())

  useEffect(() => {
    // INCHARGE and ADMIN can also view pending actions (they can apply to programs too)
    if (user?.role === 'SEWADAR' || user?.role === 'INCHARGE' || user?.role === 'ADMIN') {
      loadPendingPrograms()
    }
  }, [user])

  // Live timer tick for countdowns
  useEffect(() => {
    const t = setInterval(() => setNowMs(Date.now()), 1000)
    return () => clearInterval(t)
  }, [])

  const loadPendingPrograms = async () => {
    try {
      setLoading(true)
      // Get all programs where form is released
      const programsResponse = await api.get('/programs')
      const allPrograms = programsResponse.data

      // Get workflows to find programs with form released
      const workflowPromises = allPrograms.map((p) =>
        api.get(`/workflow/program/${p.id}`).catch(() => null)
      )
      const workflows = await Promise.all(workflowPromises)

      // Load this sewadar's applications to check APPROVED status
      const applicationsRes = await api.get(
        `/program-applications/sewadar/${user.zonalId}`,
      )
      const myApps = applicationsRes.data || []

      // Filter programs where:
      // - form is released and workflow at node >= 4
      // - sewadar has an APPROVED application
      // - sewadar hasn't submitted the form yet
      const pending = []
      for (let i = 0; i < allPrograms.length; i++) {
        const workflow = workflows[i]?.data
        if (workflow && workflow.formReleased && workflow.currentNode >= 4) {
          const programId = allPrograms[i].id
          const hasApprovedApp = myApps.some(
            (app) => app.programId === programId && app.status === 'APPROVED',
          )
          if (!hasApprovedApp) {
            continue
          }

          // Check if sewadar already submitted
          try {
            const submission = await api.get(
              `/form-submissions/program/${allPrograms[i].id}/sewadar/${user.zonalId}`
            )
            if (!submission.data) {
              pending.push(allPrograms[i])
            }
          } catch (e) {
            // No submission found, add to pending
            pending.push(allPrograms[i])
          }
        }
      }

      setPendingPrograms(pending)
    } catch (error) {
      console.error('Error loading pending programs:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleOpenForm = (program) => {
    setSelectedProgram(program)
    setOpenForm(true)
  }

  const handleCloseForm = () => {
    setOpenForm(false)
    setSelectedProgram(null)
    setFormData({
      // name removed - available from sewadar relationship
      startingDateTimeFromHome: null,
      reachingDateTimeToHome: null,
      onwardTrainFlightDateTime: null,
      onwardTrainFlightNo: '',
      returnTrainFlightDateTime: null,
      returnTrainFlightNo: '',
      stayInHotel: '',
      stayInPandal: '',
      stayDetails: '',
    })
  }

  const handleSubmit = async () => {
    try {
      setSubmitting(true)
      await api.post('/form-submissions', {
        programId: selectedProgram.id,
        ...formData,
      })
      alert('Form submitted successfully!')
      handleCloseForm()
      loadPendingPrograms()
    } catch (error) {
      alert(error.response?.data?.message || 'Failed to submit form')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box>
      <Typography variant="h4" component="h1" sx={{ fontWeight: 600, mb: 3 }}>
        Pending Actions
      </Typography>

      {pendingPrograms.length === 0 ? (
        <Alert severity="info">No pending actions at this time.</Alert>
      ) : (
        <Grid container spacing={3}>
          {pendingPrograms.map((program) => {
            const submitDeadline = parseBackendLocalDateTime(program.lastDateToSubmitForm)
            const submitCountdown = formatCountdownHm(submitDeadline, nowMs)
            const submitClosed = isPastDeadline(submitDeadline, nowMs)

            return (
              <Grid item xs={12} md={6} key={program.id}>
                <Card>
                  <CardContent>
                  <Box display="flex" alignItems="center" gap={2} mb={2}>
                    <AssignmentIcon color="primary" />
                    <Typography variant="h6" sx={{ fontWeight: 600 }}>
                      {program.title}
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    Please fill the travel details form for this program.
                  </Typography>
                  {submitDeadline && (
                    <Typography
                      variant="caption"
                      color={submitClosed ? 'error.main' : 'text.secondary'}
                      sx={{ display: 'block', mt: 1, fontWeight: 600 }}
                    >
                      {submitClosed
                        ? 'Form submission deadline passed'
                        : `Time left to submit form: ${submitCountdown}`}
                    </Typography>
                  )}
                  <Button
                    variant="contained"
                    fullWidth
                    sx={{ mt: 2 }}
                    onClick={() => handleOpenForm(program)}
                    disabled={submitClosed}
                  >
                    Fill Form
                  </Button>
                  </CardContent>
                </Card>
              </Grid>
            )
          })}
        </Grid>
      )}

      {/* Form Dialog */}
      <Dialog open={openForm} onClose={handleCloseForm} maxWidth="md" fullWidth>
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box component="span">Travel Details Form - {selectedProgram?.title}</Box>
          <IconButton
            size="small"
            onClick={handleCloseForm}
            aria-label="Close"
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ maxHeight: '70vh', overflowY: 'auto' }}>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            {/* Name field removed - available from sewadar relationship */}
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Starting Date and time from Home"
                value={toLocalDatetimeString(formData.startingDateTimeFromHome)}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    startingDateTimeFromHome: e.target.value ? new Date(e.target.value) : null,
                  })
                }
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Reaching Date and time to Home"
                value={toLocalDatetimeString(formData.reachingDateTimeToHome)}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    reachingDateTimeToHome: e.target.value ? new Date(e.target.value) : null,
                  })
                }
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="subtitle2" sx={{ mt: 2, mb: 1, fontWeight: 600 }}>
                Journey Details
              </Typography>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Onward Train/Flight date and time"
                value={toLocalDatetimeString(formData.onwardTrainFlightDateTime)}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    onwardTrainFlightDateTime: e.target.value ? new Date(e.target.value) : null,
                  })
                }
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Onward Train/Flight No."
                value={formData.onwardTrainFlightNo}
                onChange={(e) =>
                  setFormData({ ...formData, onwardTrainFlightNo: e.target.value })
                }
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Return Train/Flight date and time"
                value={toLocalDatetimeString(formData.returnTrainFlightDateTime)}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    returnTrainFlightDateTime: e.target.value ? new Date(e.target.value) : null,
                  })
                }
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Return Train/Flight No."
                value={formData.returnTrainFlightNo}
                onChange={(e) =>
                  setFormData({ ...formData, returnTrainFlightNo: e.target.value })
                }
              />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="subtitle2" sx={{ mt: 2, mb: 1, fontWeight: 600 }}>
                Stay Details
              </Typography>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Stay Details"
                multiline
                rows={3}
                value={formData.stayDetails}
                onChange={(e) => setFormData({ ...formData, stayDetails: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseForm} disabled={submitting}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} variant="contained" disabled={submitting}>
            {submitting ? 'Submitting...' : 'Submit'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default PendingActions

