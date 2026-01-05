import { useState, useEffect } from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  InputAdornment,
  Chip,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  IconButton,
  CircularProgress,
  Alert,
  Pagination,
  Stack,
} from '@mui/material'
import {
  Search as SearchIcon,
  Add as AddIcon,
  Edit as EditIcon,
  Event as EventIcon,
} from '@mui/icons-material'
import { format } from 'date-fns'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import ProgramForm from '../components/ProgramForm'

const Programs = () => {
  const { user } = useAuth()
  const [programs, setPrograms] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [openForm, setOpenForm] = useState(false)
  const [selectedProgram, setSelectedProgram] = useState(null)
  const [myApplications, setMyApplications] = useState([])
  const itemsPerPage = 12

  useEffect(() => {
    loadPrograms()
    if (user?.role === 'SEWADAR') {
      loadMyApplications()
    }
  }, [user])

  const loadPrograms = async () => {
    try {
      setLoading(true)
      const response = await api.get('/programs')
      setPrograms(response.data)
      setTotalPages(Math.ceil(response.data.length / itemsPerPage))
    } catch (error) {
      console.error('Error loading programs:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadMyApplications = async () => {
    try {
      const response = await api.get(`/program-applications/sewadar/${user.zonalId}`)
      setMyApplications(response.data)
    } catch (error) {
      console.error('Error loading applications:', error)
    }
  }

  const handleApply = async (programId) => {
    try {
      await api.post('/program-applications', {
        programId,
        sewadarId: user.zonalId,
      })
      await loadMyApplications()
      alert('Application submitted successfully!')
    } catch (error) {
      alert(error.response?.data?.message || 'Failed to apply')
    }
  }

  const filteredPrograms = programs.filter((program) => {
    const matchesSearch =
      program.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      program.location?.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesStatus = statusFilter === 'all' || program.status === statusFilter
    return matchesSearch && matchesStatus
  })

  const paginatedPrograms = filteredPrograms.slice(
    (page - 1) * itemsPerPage,
    page * itemsPerPage
  )

  const getApplicationStatus = (programId) => {
    const app = myApplications.find((a) => a.programId === programId)
    return app?.status
  }

  const handleEdit = (program) => {
    setSelectedProgram(program)
    setOpenForm(true)
  }

  const handleCloseForm = () => {
    setOpenForm(false)
    setSelectedProgram(null)
    loadPrograms()
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
          Programs
        </Typography>
        {user?.role === 'INCHARGE' && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenForm(true)}
          >
            Create Program
          </Button>
        )}
      </Box>

      <Box display="flex" gap={2} mb={3}>
        <TextField
          placeholder="Search programs..."
          value={searchTerm}
          onChange={(e) => {
            setSearchTerm(e.target.value)
            setPage(1)
          }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
          sx={{ flex: 1 }}
        />
        <FormControl sx={{ minWidth: 150 }}>
          <InputLabel>Status</InputLabel>
          <Select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value)
              setPage(1)
            }}
            label="Status"
          >
            <MenuItem value="all">All</MenuItem>
            <MenuItem value="scheduled">Scheduled</MenuItem>
            <MenuItem value="active">Active</MenuItem>
            <MenuItem value="cancelled">Cancelled</MenuItem>
          </Select>
        </FormControl>
      </Box>

      {filteredPrograms.length === 0 ? (
        <Alert severity="info">No programs found matching your criteria.</Alert>
      ) : (
        <>
          <Grid container spacing={3}>
            {paginatedPrograms.map((program) => {
              const appStatus = getApplicationStatus(program.id)
              return (
                <Grid item xs={12} sm={6} md={4} key={program.id}>
                  <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                    <CardContent sx={{ flexGrow: 1 }}>
                      <Box display="flex" justifyContent="space-between" alignItems="start" mb={2}>
                        <Typography variant="h6" component="h3" sx={{ fontWeight: 600 }}>
                          {program.title}
                        </Typography>
                        {user?.role === 'INCHARGE' && (
                          <IconButton
                            size="small"
                            onClick={() => handleEdit(program)}
                            sx={{ ml: 1 }}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                        )}
                      </Box>

                      <Box mb={2}>
                        <Chip
                          label={program.status}
                          size="small"
                          color={
                            program.status === 'active'
                              ? 'success'
                              : program.status === 'cancelled'
                              ? 'error'
                              : 'default'
                          }
                          sx={{ mb: 1 }}
                        />
                        <Typography variant="body2" color="text.secondary">
                          <strong>Location:</strong> {program.location} ({program.locationType})
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          <strong>Dates:</strong>{' '}
                          {program.programDates?.length > 0
                            ? program.programDates
                                .map((d) => format(new Date(d), 'MMM dd, yyyy'))
                                .join(', ')
                            : 'N/A'}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          <strong>Applications:</strong> {program.applicationCount || 0}
                          {program.maxSewadars && ` / ${program.maxSewadars}`}
                        </Typography>
                      </Box>

                      {user?.role === 'SEWADAR' && program.status === 'active' && (
                        <Box mt="auto">
                          {appStatus ? (
                            <Chip
                              label={appStatus}
                              color={appStatus === 'APPROVED' ? 'success' : 'default'}
                              size="small"
                            />
                          ) : (
                            <Button
                              variant="contained"
                              fullWidth
                              size="small"
                              onClick={() => handleApply(program.id)}
                            >
                              Apply
                            </Button>
                          )}
                        </Box>
                      )}
                    </CardContent>
                  </Card>
                </Grid>
              )
            })}
          </Grid>

          {totalPages > 1 && (
            <Box display="flex" justifyContent="center" mt={4}>
              <Pagination
                count={totalPages}
                page={page}
                onChange={(e, value) => setPage(value)}
                color="primary"
              />
            </Box>
          )}

          <Typography variant="body2" color="text.secondary" mt={2} textAlign="center">
            Showing {paginatedPrograms.length} of {filteredPrograms.length} programs
          </Typography>
        </>
      )}

      <Dialog open={openForm} onClose={handleCloseForm} maxWidth="md" fullWidth>
        <DialogTitle>
          {selectedProgram ? 'Edit Program' : 'Create Program'}
        </DialogTitle>
        <DialogContent>
          <ProgramForm
            program={selectedProgram}
            onClose={handleCloseForm}
            onSuccess={handleCloseForm}
          />
        </DialogContent>
      </Dialog>
    </Box>
  )
}

export default Programs

