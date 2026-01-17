import { useState, useEffect } from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  Button,
  CircularProgress,
  Alert,
  Grid,
} from '@mui/material'
import { format } from 'date-fns'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const Applications = () => {
  const { user } = useAuth()
  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadApplications()
  }, [])

  const loadApplications = async () => {
    try {
      const response = await api.get(`/program-applications/sewadar/${user.zonalId}`)
      // Filter out applications for completed programs
      const applicationsWithPrograms = await Promise.all(
        response.data.map(async (app) => {
          try {
            const programResponse = await api.get(`/programs/${app.programId}`)
            return {
              ...app,
              programStatus: programResponse.data.status,
            }
          } catch (error) {
            return { ...app, programStatus: null }
          }
        })
      )
      
      // Filter out completed programs
      const filtered = applicationsWithPrograms.filter((app) => app.programStatus !== 'completed')
      setApplications(filtered)
    } catch (error) {
      console.error('Error loading applications:', error)
    } finally {
      setLoading(false)
    }
  }
  
  const handleReapply = async (programId) => {
    if (!window.confirm('Reapply to this program?')) {
      return
    }

    try {
      await api.post('/program-applications', {
        programId,
        sewadarId: user.zonalId,
      })
      alert('Application submitted successfully!')
      loadApplications()
    } catch (error) {
      alert(error.response?.data?.message || 'Failed to reapply')
    }
  }

  const handleRequestDrop = async (applicationId) => {
    if (!window.confirm('Request to drop from this program? This requires incharge approval.')) {
      return
    }

    try {
      await api.put(`/program-applications/${applicationId}/request-drop?sewadarId=${user.zonalId}`)
      alert('Drop request submitted. Waiting for incharge approval.')
      loadApplications()
    } catch (error) {
      alert(error.response?.data?.message || 'Failed to request drop')
    }
  }

  const getStatusColor = (status) => {
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
        My Applications
      </Typography>

      {applications.length === 0 ? (
        <Alert severity="info">You haven't applied to any programs yet.</Alert>
      ) : (
        <Grid container spacing={3}>
          {applications.map((app) => (
            <Grid item xs={12} md={6} key={app.id}>
              <Card>
                <CardContent>
                  <Box display="flex" justifyContent="space-between" alignItems="start" mb={2}>
                    <Typography variant="h6" component="h3" sx={{ fontWeight: 600 }}>
                      {app.programTitle}
                    </Typography>
                    <Chip
                      label={app.status}
                      color={getStatusColor(app.status)}
                      size="small"
                    />
                  </Box>

                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>Applied:</strong> {format(new Date(app.appliedAt), 'MMM dd, yyyy')}
                  </Typography>

                  {app.dropRequestedAt && (
                    <Typography variant="body2" color="text.secondary" gutterBottom>
                      <strong>Drop Requested:</strong>{' '}
                      {format(new Date(app.dropRequestedAt), 'MMM dd, yyyy')}
                    </Typography>
                  )}

                  {app.dropApprovedAt && (
                    <Typography variant="body2" color="text.secondary" gutterBottom>
                      <strong>Drop Approved:</strong>{' '}
                      {format(new Date(app.dropApprovedAt), 'MMM dd, yyyy')}
                    </Typography>
                  )}

                  <Box mt={2} display="flex" gap={1}>
                    {(app.status === 'PENDING' || app.status === 'APPROVED') && (
                      <Button
                        variant="outlined"
                        color="error"
                        size="small"
                        onClick={() => handleRequestDrop(app.id)}
                      >
                        Request Drop
                      </Button>
                    )}
                    {app.status === 'DROP_REQUESTED' && (
                      <Chip label="Drop Request Pending" color="error" size="small" />
                    )}
                    {app.status === 'DROPPED' && (
                      <Button
                        variant="contained"
                        color="primary"
                        size="small"
                        onClick={() => handleReapply(app.programId)}
                      >
                        Reapply
                      </Button>
                    )}
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}
    </Box>
  )
}

export default Applications

