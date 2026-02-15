import { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  Switch,
  FormControlLabel,
  Stepper,
  Step,
  StepLabel,
  Alert,
  CircularProgress,
  Grid,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material'
import {
  CheckCircle as CheckCircleIcon,
  RadioButtonUnchecked as RadioButtonUncheckedIcon,
  ArrowForward as ArrowForwardIcon,
} from '@mui/icons-material'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const Workflow = () => {
  const { user } = useAuth()
  const [programs, setPrograms] = useState([])
  const [workflows, setWorkflows] = useState({})
  const [globalPreferences, setGlobalPreferences] = useState([])
  const [programPreferences, setProgramPreferences] = useState({}) // { programId: [preferences] }
  const [loading, setLoading] = useState(true)
  const [selectedProgram, setSelectedProgram] = useState(null)
  const [openWorkflowDialog, setOpenWorkflowDialog] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (user?.role === 'INCHARGE') {
      loadData()
    }
  }, [user])

  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)
      
      // Load programs and global preferences
      // For INCHARGE role, show ALL programs (not just ones they created)
      // This allows promoted incharges to see all programs in the system
      // Note: Only program creators can manage workflows (enforced by backend)
      const [programsRes, preferencesRes] = await Promise.all([
        api.get('/programs').catch(err => {
          console.error('Error loading programs:', err)
          return { data: [] }
        }),
        api.get('/notification-preferences').catch(err => {
          console.error('Error loading global preferences:', err)
          return { data: [] }
        }),
      ])
      
      setPrograms(programsRes.data || [])
      setGlobalPreferences(preferencesRes.data || [])

      // Load workflows for all programs
      const workflowPromises = (programsRes.data || []).map((p) =>
        api.get(`/workflow/program/${p.id}`)
          .then((r) => r.data)
          .catch(err => {
            console.error(`Error loading workflow for program ${p.id}:`, err)
            return null
          })
      )
      const workflowData = await Promise.all(workflowPromises)
      const workflowMap = {}
      workflowData.forEach((w) => {
        if (w) {
          workflowMap[w.programId] = w
        }
      })
      setWorkflows(workflowMap)

      // Load program-level preferences for each program
      const programPrefPromises = (programsRes.data || []).map((p) =>
        api.get(`/program-notification-preferences/program/${p.id}`)
          .then((r) => ({ programId: p.id, preferences: r.data }))
          .catch(err => {
            console.error(`Error loading preferences for program ${p.id}:`, err)
            return { programId: p.id, preferences: [] }
          })
      )
      const programPrefData = await Promise.all(programPrefPromises)
      const programPrefMap = {}
      programPrefData.forEach(({ programId, preferences }) => {
        programPrefMap[programId] = preferences
      })
      setProgramPreferences(programPrefMap)
    } catch (error) {
      console.error('Error loading data:', error)
      setError('Failed to load workflow data. Please refresh the page.')
    } finally {
      setLoading(false)
    }
  }

  const handleToggleGlobalPreference = async (preferenceId, enabled) => {
    try {
      await api.put(`/notification-preferences/${preferenceId}/toggle?enabled=${!enabled}`)
      setGlobalPreferences((prev) =>
        prev.map((p) => (p.id === preferenceId ? { ...p, enabled: !enabled } : p))
      )
      // Reload program preferences to update effective values
      await loadData()
    } catch (error) {
      alert('Failed to update global preference')
    }
  }

  const handleToggleProgramPreference = async (programId, nodeNumber, currentEnabled) => {
    try {
      // Toggle: null -> true -> false -> null (cycle)
      let newValue = null
      if (currentEnabled === null) {
        newValue = true
      } else if (currentEnabled === true) {
        newValue = false
      } else {
        newValue = null // Use global
      }

      await api.put(`/program-notification-preferences/program/${programId}/node/${nodeNumber}`, null, {
        params: { enabled: newValue }
      })
      
      // Reload program preferences
      const res = await api.get(`/program-notification-preferences/program/${programId}`)
      setProgramPreferences((prev) => ({
        ...prev,
        [programId]: res.data
      }))
    } catch (error) {
      console.error('Error updating program preference:', error)
      alert('Failed to update program preference')
    }
  }

  const handleMoveToNextNode = async (programId) => {
    try {
      await api.post(`/workflow/program/${programId}/next-node`)
      await loadData()
      alert('Moved to next step successfully!')
    } catch (error) {
      alert('Failed to move to next step')
    }
  }

  const handleReleaseForm = async (programId) => {
    try {
      await api.post(`/workflow/program/${programId}/release-form`)
      await loadData()
      alert('Form released successfully!')
    } catch (error) {
      alert('Failed to release form')
    }
  }

  const handleMarkDetailsCollected = async (programId) => {
    try {
      await api.post(`/workflow/program/${programId}/mark-details-collected`)
      await loadData()
      alert('Details marked as collected!')
    } catch (error) {
      alert('Failed to mark details collected')
    }
  }

  // Step names matching the backend
  const STEP_NAMES = [
    'Make Program Active',
    'Post Application Message',
    'Release Form',
    'Collect Details',
    'Post Mail to Area Secretary',
    'Post General Instructions'
  ]

  const getStepName = (nodeNumber) => {
    // First try to get from global preferences (if loaded)
    const prefName = globalPreferences.find((p) => p.nodeNumber === nodeNumber)?.nodeName
    if (prefName) {
      return prefName
    }
    // Fallback to hardcoded names matching backend
    if (nodeNumber >= 1 && nodeNumber <= STEP_NAMES.length) {
      return STEP_NAMES[nodeNumber - 1]
    }
    // Last resort fallback
    return `Step ${nodeNumber}`
  }

  const getProgramPreference = (programId, nodeNumber) => {
    const prefs = programPreferences[programId] || []
    return prefs.find(p => p.nodeNumber === nodeNumber)
  }

  const getNodeActions = (workflow) => {
    const node = workflow.currentNode
    if (node === 3 && !workflow.formReleased) {
      return (
        <Button
          variant="contained"
          color="primary"
          onClick={() => handleReleaseForm(workflow.programId)}
        >
          Release Form
        </Button>
      )
    } else if (node === 4 && !workflow.detailsCollected) {
      return (
        <Button
          variant="contained"
          color="primary"
          onClick={() => handleMarkDetailsCollected(workflow.programId)}
        >
          Mark Details Collected
        </Button>
      )
    } else if (node < 6) {
      return (
        <Button
          variant="contained"
          color="primary"
          startIcon={<ArrowForwardIcon />}
          onClick={() => handleMoveToNextNode(workflow.programId)}
        >
          Go to Next Step
        </Button>
      )
    }
    return <Chip label="Workflow Complete" color="success" />
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
        Program Workflow Management
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Global Notification Preferences */}
      <Card sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Notification Preferences (Global)
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            These settings apply to all programs unless overridden at the program level.
          </Typography>
          <Grid container spacing={2}>
            {globalPreferences.map((pref) => (
              <Grid item xs={12} sm={6} md={4} key={pref.id}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={pref.enabled || false}
                      onChange={() => handleToggleGlobalPreference(pref.id, pref.enabled)}
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body1" fontWeight={600}>
                        {pref.nodeName}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Step {pref.nodeNumber}
                      </Typography>
                    </Box>
                  }
                />
              </Grid>
            ))}
          </Grid>
        </CardContent>
      </Card>

      {/* Program Workflows */}
      <Typography variant="h6" gutterBottom sx={{ mb: 2 }}>
        Program Workflows
      </Typography>

      {programs.length === 0 ? (
        <Alert severity="info">No programs found. Create a program to see its workflow.</Alert>
      ) : (
        <Grid container spacing={3}>
          {programs.map((program) => {
            const workflow = workflows[program.id]
            const programPrefs = programPreferences[program.id] || []

            return (
              <Grid item xs={12} key={program.id}>
                <Card>
                  <CardContent>
                    <Box display="flex" justifyContent="space-between" alignItems="start" mb={2}>
                      <Box>
                        <Typography variant="h6" sx={{ fontWeight: 600 }}>
                          {program.title}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {program.location}
                        </Typography>
                      </Box>
                      <Chip
                        label={program.status}
                        size="small"
                        color={program.status === 'active' ? 'success' : 'default'}
                      />
                    </Box>

                    {!workflow ? (
                      <Alert severity="warning">
                        Workflow not initialized. Please refresh the page or contact support.
                      </Alert>
                    ) : (
                      <>
                        <Stepper activeStep={workflow.currentNode - 1} orientation="vertical" sx={{ mb: 3 }}>
                          {[1, 2, 3, 4, 5, 6].map((nodeNum) => {
                            const nodePref = getProgramPreference(program.id, nodeNum)
                            const effectiveEnabled = nodePref?.effectiveEnabled ?? false
                            
                            return (
                              <Step key={nodeNum}>
                                <StepLabel
                                  StepIconComponent={() =>
                                    nodeNum < workflow.currentNode ? (
                                      <CheckCircleIcon color="success" />
                                    ) : nodeNum === workflow.currentNode ? (
                                      <RadioButtonUncheckedIcon color="primary" />
                                    ) : (
                                      <RadioButtonUncheckedIcon color="disabled" />
                                    )
                                  }
                                >
                                  <Box display="flex" alignItems="center" justifyContent="space-between">
                                    <Box>
                                      {getStepName(nodeNum)}
                                      {nodeNum === workflow.currentNode && (
                                        <Chip label="Current" size="small" color="primary" sx={{ ml: 1 }} />
                                      )}
                                    </Box>
                                    <FormControlLabel
                                      control={
                                        <Switch
                                          size="small"
                                          checked={effectiveEnabled}
                                          onChange={() => handleToggleProgramPreference(
                                            program.id,
                                            nodeNum,
                                            nodePref?.enabled ?? null
                                          )}
                                        />
                                      }
                                      label={
                                        <Typography variant="caption">
                                          {nodePref?.enabled === null ? 'Global' : 
                                           nodePref?.enabled ? 'On' : 'Off'}
                                        </Typography>
                                      }
                                      sx={{ ml: 2 }}
                                    />
                                  </Box>
                                </StepLabel>
                              </Step>
                            )
                          })}
                        </Stepper>

                        <Box mt={2} display="flex" gap={2} flexWrap="wrap">
                          {getNodeActions(workflow)}
                        </Box>
                      </>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            )
          })}
        </Grid>
      )}
    </Box>
  )
}

export default Workflow

