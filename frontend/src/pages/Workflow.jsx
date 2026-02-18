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
  TextField,
  IconButton,
} from '@mui/material'
import {
  CheckCircle as CheckCircleIcon,
  RadioButtonUnchecked as RadioButtonUncheckedIcon,
  ArrowForward as ArrowForwardIcon,
  Send as SendIcon,
  Edit as EditIcon,
  Close as CloseIcon,
} from '@mui/icons-material'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { isAdminOrIncharge } from '../utils/roleUtils'

const STEP_NAMES = [
  'Make Program Active',
  'Post Application Message',
  'Release Form',
  'Collect Details',
  'Post Mail to Area Secretary',
  'Post General Instructions',
]

const Workflow = () => {
  const { user } = useAuth()
  const [programs, setPrograms] = useState([])
  const [workflows, setWorkflows] = useState({})
  const [programPreferences, setProgramPreferences] = useState({}) // { programId: [preferences] }
  const [loading, setLoading] = useState(true)
  const [selectedProgram, setSelectedProgram] = useState(null)
  const [openWorkflowDialog, setOpenWorkflowDialog] = useState(false)
  const [error, setError] = useState(null)
  const [missingForms, setMissingForms] = useState([])
  const [openMissingDialog, setOpenMissingDialog] = useState(false)
  const [triggeringNotifications, setTriggeringNotifications] = useState(false)
  const [openMessageDialog, setOpenMessageDialog] = useState(false)
  const [editingNode, setEditingNode] = useState(null)
  const [editingProgramId, setEditingProgramId] = useState(null)
  const [messageText, setMessageText] = useState('')
  const [defaultMessage, setDefaultMessage] = useState('')
  const [showArchived, setShowArchived] = useState(false)

  useEffect(() => {
    if (isAdminOrIncharge(user)) {
      loadData()
    }
  }, [user, showArchived])

  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)

      // Load programs
      // For INCHARGE role, show ALL programs (not just ones they created)
      // This allows promoted incharges to see all programs in the system
      // Note: Only program creators can manage workflows (enforced by backend)
      const programsRes = await api.get('/programs').catch((err) => {
        console.error('Error loading programs:', err)
        return { data: [] }
      })

      setPrograms(programsRes.data || [])

      // Load workflows for incharge with archived filter
      const workflowsRes = await api.get(`/workflow/incharge/${user.zonalId}?archived=${showArchived}`).catch((err) => {
        console.error('Error loading workflows:', err)
        return { data: [] }
      })

      const workflowData = workflowsRes.data || []
      const workflowMap = {}
      workflowData.forEach((w) => {
        if (w) {
          workflowMap[w.programId] = w
        }
      })
      setWorkflows(workflowMap)

      // Filter programs to only show those with workflows (matching archived filter)
      const programIdsWithWorkflows = new Set(workflowData.map((w) => w.programId))
      const filteredPrograms = (programsRes.data || []).filter((p) =>
        programIdsWithWorkflows.has(p.id),
      )
      setPrograms(filteredPrograms)

      // Load program-level preferences for each program (use filtered programs)
      const programPrefPromises = filteredPrograms.map((p) =>
        api
          .get(`/program-notification-preferences/program/${p.id}`)
          .then((r) => ({ programId: p.id, preferences: r.data }))
          .catch((err) => {
            console.error(`Error loading preferences for program ${p.id}:`, err)
            return { programId: p.id, preferences: [] }
          }),
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

  const handleToggleProgramPreference = async (programId, nodeNumber, currentEnabled) => {
    try {
      // Simple toggle: true <-> false
      const newValue = !currentEnabled

      await api.put(
        `/program-notification-preferences/program/${programId}/node/${nodeNumber}`,
        null,
        {
          params: { enabled: newValue },
        },
      )

      // Reload program preferences
      const res = await api.get(`/program-notification-preferences/program/${programId}`)
      setProgramPreferences((prev) => ({
        ...prev,
        [programId]: res.data,
      }))
    } catch (error) {
      console.error('Error updating program preference:', error)
      alert('Failed to update program preference')
    }
  }

  const handleOpenMessageDialog = (programId, nodeNumber) => {
    const pref = getProgramPreference(programId, nodeNumber)
    setEditingProgramId(programId)
    setEditingNode(nodeNumber)
    // Show custom message if available, otherwise show default (for editing)
    setMessageText(pref?.isCustomMessage ? pref.message : '')
    setDefaultMessage(pref?.defaultMessage || '')
    setOpenMessageDialog(true)
  }

  const handleCloseMessageDialog = () => {
    setOpenMessageDialog(false)
    setEditingProgramId(null)
    setEditingNode(null)
    setMessageText('')
    setDefaultMessage('')
  }

  const handleSaveMessage = async () => {
    if (!editingProgramId || !editingNode) return

    try {
      await api.put(
        `/program-notification-preferences/program/${editingProgramId}/node/${editingNode}`,
        null,
        {
          params: { message: messageText.trim() || null },
        },
      )

      // Reload program preferences
      const res = await api.get(`/program-notification-preferences/program/${editingProgramId}`)
      setProgramPreferences((prev) => ({
        ...prev,
        [editingProgramId]: res.data,
      }))

      handleCloseMessageDialog()
      alert('Message updated successfully!')
    } catch (error) {
      console.error('Error updating message:', error)
      alert('Failed to update message: ' + (error.response?.data?.message || error.message))
    }
  }

  const handleResetMessage = async () => {
    if (!editingProgramId || !editingNode) return

    try {
      await api.post(
        `/program-notification-preferences/program/${editingProgramId}/node/${editingNode}/reset-message`,
      )

      // Reload program preferences
      const res = await api.get(`/program-notification-preferences/program/${editingProgramId}`)
      setProgramPreferences((prev) => ({
        ...prev,
        [editingProgramId]: res.data,
      }))

      // Update dialog to show default message
      const pref = res.data.find((p) => p.nodeNumber === editingNode)
      setMessageText(pref?.defaultMessage || '')
      alert('Message reset to default!')
    } catch (error) {
      console.error('Error resetting message:', error)
      alert('Failed to reset message: ' + (error.response?.data?.message || error.message))
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
      // First check if there are missing form submissions
      const res = await api.get(`/workflow/program/${programId}/missing-forms`)
      const missing = res.data || []

      if (missing.length > 0) {
        setMissingForms(missing)
        setSelectedProgram(programs.find((p) => p.id === programId) || null)
        setOpenMissingDialog(true)
        return
      }

      await api.post(`/workflow/program/${programId}/mark-details-collected`)
      await loadData()
      alert('Details marked as collected!')
    } catch (error) {
      const message = error.response?.data?.message || 'Failed to mark details collected'
      alert(message)
    }
  }

  const getStepName = (nodeNumber) => {
    if (nodeNumber >= 1 && nodeNumber <= STEP_NAMES.length) {
      return STEP_NAMES[nodeNumber - 1]
    }
    return `Step ${nodeNumber}`
  }

  const getProgramPreference = (programId, nodeNumber) => {
    const prefs = programPreferences[programId] || []
    return prefs.find((p) => p.nodeNumber === nodeNumber)
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
    // If archived, show unarchive button, otherwise show archive button
    if (workflow.archived) {
      return (
        <Button
          variant="outlined"
          color="secondary"
          onClick={() => handleUnarchiveWorkflow(workflow.programId)}
        >
          Unarchive Workflow
        </Button>
      )
    }
    return (
      <Button
        variant="contained"
        color="success"
        onClick={() => handleArchiveWorkflow(workflow.programId)}
      >
        Complete & Archive
      </Button>
    )
  }

  const handleArchiveWorkflow = async (programId) => {
    if (!window.confirm('Are you sure you want to complete and archive this workflow? This will mark it as finished.')) {
      return
    }

    try {
      await api.post(`/workflow/program/${programId}/archive`)
      await loadData()
      alert('Workflow archived successfully!')
    } catch (error) {
      const message = error.response?.data?.message || 'Failed to archive workflow'
      alert(message)
    }
  }

  const handleUnarchiveWorkflow = async (programId) => {
    if (!window.confirm('Are you sure you want to unarchive this workflow? It will be restored to active status.')) {
      return
    }

    try {
      await api.post(`/workflow/program/${programId}/unarchive`)
      await loadData()
      alert('Workflow unarchived successfully!')
    } catch (error) {
      const message = error.response?.data?.message || 'Failed to unarchive workflow'
      alert(message)
    }
  }

  const handleTriggerNotifications = async () => {
    if (!window.confirm('Trigger workflow notifications for all programs now? This will send notifications to all programs at their current workflow step (same as the 9:00 AM scheduler).')) {
      return
    }

    try {
      setTriggeringNotifications(true)
      const response = await api.post('/workflow/trigger-notifications')
      if (response.data.success) {
        alert('✅ Notifications triggered successfully! ' + (response.data.message || ''))
      } else {
        alert('❌ Failed to trigger notifications: ' + (response.data.message || 'Unknown error'))
      }
    } catch (error) {
      console.error('Error triggering notifications:', error)
      alert('❌ Error triggering notifications: ' + (error.response?.data?.message || error.message))
    } finally {
      setTriggeringNotifications(false)
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
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1" sx={{ fontWeight: 600 }}>
          Program Workflow Management
        </Typography>
        <Box display="flex" gap={2} alignItems="center">
          <FormControlLabel
            control={
              <Switch
                checked={showArchived}
                onChange={(e) => setShowArchived(e.target.checked)}
                color="primary"
              />
            }
            label={showArchived ? 'Show Archived' : 'Show Active'}
          />
          <Button
            variant="contained"
            color="primary"
            startIcon={<SendIcon />}
            onClick={handleTriggerNotifications}
            disabled={triggeringNotifications || loading}
            sx={{ minWidth: 200 }}
          >
            {triggeringNotifications ? 'Triggering...' : 'Trigger Notifications'}
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

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

            return (
              <Grid item xs={12} key={program.id}>
                <Card>
                  <CardContent>
                    <Box display="flex" justifyContent="space-between" alignItems="start" mb={2}>
                      <Box>
                        <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                          <Typography variant="h6" sx={{ fontWeight: 600 }}>
                            {program.title}
                          </Typography>
                          {workflow?.archived && (
                            <Chip
                              label="Archived"
                              size="small"
                              color="secondary"
                              sx={{ fontSize: '0.7rem' }}
                            />
                          )}
                        </Box>
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
                        <Stepper
                          activeStep={workflow.currentNode - 1}
                          orientation="vertical"
                          sx={{ mb: 3 }}
                        >
                          {[1, 2, 3, 4, 5, 6].map((nodeNum) => {
                            const nodePref = getProgramPreference(program.id, nodeNum)
                            const enabled = nodePref?.enabled ?? false

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
                                  <Box
                                    display="flex"
                                    alignItems="center"
                                    justifyContent="space-between"
                                  >
                                    <Box>
                                      {getStepName(nodeNum)}
                                      {nodeNum === workflow.currentNode && (
                                        <Chip
                                          label="Current"
                                          size="small"
                                          color="primary"
                                          sx={{ ml: 1 }}
                                        />
                                      )}
                                    </Box>
                                    <Box display="flex" alignItems="center" gap={1}>
                                      <IconButton
                                        size="small"
                                        onClick={() => handleOpenMessageDialog(program.id, nodeNum)}
                                        title="Edit message"
                                        sx={{ color: 'primary.main' }}
                                      >
                                        <EditIcon fontSize="small" />
                                      </IconButton>
                                      <FormControlLabel
                                        control={
                                          <Switch
                                            size="small"
                                            checked={!!enabled}
                                            onChange={() =>
                                              handleToggleProgramPreference(
                                                program.id,
                                                nodeNum,
                                                !!enabled,
                                              )
                                            }
                                          />
                                        }
                                        label={
                                          <Typography variant="caption">
                                            {enabled ? 'On' : 'Off'}
                                          </Typography>
                                        }
                                        sx={{ ml: 0 }}
                                      />
                                    </Box>
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

      {/* Missing form submissions dialog */}
      <Dialog
        open={openMissingDialog}
        onClose={() => setOpenMissingDialog(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box component="span">
            Missing Form Submissions{selectedProgram ? ` - ${selectedProgram.title}` : ''}
          </Box>
          <IconButton size="small" onClick={() => setOpenMissingDialog(false)}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ maxHeight: '70vh', overflowY: 'auto' }}>
          {missingForms.length === 0 ? (
            <Alert severity="info">All approved sewadars have submitted their forms.</Alert>
          ) : (
            <>
              <Alert severity="warning" sx={{ mb: 2 }}>
                The following approved sewadars have not submitted their forms. You cannot move to
                the next step until all have submitted.
              </Alert>
              {missingForms.map((s) => (
                <Box
                  key={s.zonalId}
                  sx={{
                    p: 1.5,
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1,
                    mb: 1,
                  }}
                >
                  <Typography variant="body1" sx={{ fontWeight: 500 }}>
                    {s.firstName} {s.lastName} ({s.zonalId})
                  </Typography>
                  {s.mobile && (
                    <Typography variant="caption" color="text.secondary">
                      Mobile: {s.mobile}
                    </Typography>
                  )}
                </Box>
              ))}
            </>
          )}
        </DialogContent>
        <DialogActions>
          {missingForms.length > 0 && selectedProgram && (
            <Button
              onClick={async () => {
                try {
                  await api.post(`/workflow/program/${selectedProgram.id}/notify-missing-forms`)
                  alert('Notification sent to all pending sewadars.')
                } catch (error) {
                  alert('Failed to send notifications')
                }
              }}
            >
              Notify All via WhatsApp
            </Button>
          )}
          <Button onClick={() => setOpenMissingDialog(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Message Editing Dialog */}
      <Dialog
        open={openMessageDialog}
        onClose={handleCloseMessageDialog}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box component="span">
            Edit Notification Message - {editingNode && STEP_NAMES[editingNode - 1]}
          </Box>
          <IconButton size="small" onClick={handleCloseMessageDialog}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ maxHeight: '70vh', overflowY: 'auto' }}>
          <Alert severity="info" sx={{ mb: 2 }}>
            Customize the notification message for this workflow step. Use {'{programTitle}'} as a
            placeholder for the program title.
          </Alert>

          <TextField
            fullWidth
            multiline
            rows={6}
            label="Default Message"
            value={defaultMessage}
            disabled
            variant="outlined"
            sx={{ mb: 2 }}
            helperText="This is the default message. You can customize it below."
          />

          <TextField
            fullWidth
            multiline
            rows={6}
            label="Custom Message (leave empty to use default)"
            value={messageText}
            onChange={(e) => setMessageText(e.target.value)}
            variant="outlined"
            placeholder={defaultMessage}
            helperText={
              messageText.trim() === defaultMessage || messageText.trim() === ''
                ? 'Currently using default message'
                : 'Custom message will be used for notifications'
            }
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleResetMessage} color="secondary">
            Reset to Default
          </Button>
          <Button onClick={handleCloseMessageDialog}>Cancel</Button>
          <Button onClick={handleSaveMessage} variant="contained" color="primary">
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default Workflow

