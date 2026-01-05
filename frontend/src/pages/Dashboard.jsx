import { useState } from 'react'
import {
  Box,
  Tabs,
  Tab,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  CircularProgress,
  Alert,
  Typography,
  Grid,
  Card,
  CardContent,
} from '@mui/material'
import {
  Search as SearchIcon,
  Download as DownloadIcon,
} from '@mui/icons-material'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const Dashboard = () => {
  const { user } = useAuth()
  const [activeTab, setActiveTab] = useState(0)
  const [sewadars, setSewadars] = useState([])
  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(false)
  const [sewadarsPage, setSewadarsPage] = useState(0)
  const [sewadarsRowsPerPage, setSewadarsRowsPerPage] = useState(25)
  const [applicationsPage, setApplicationsPage] = useState(0)
  const [applicationsRowsPerPage, setApplicationsRowsPerPage] = useState(25)
  const [totalSewadars, setTotalSewadars] = useState(0)
  const [totalApplications, setTotalApplications] = useState(0)

  // Sewadars filters
  const [sewadarsFilters, setSewadarsFilters] = useState({
    location: '',
    languages: '',
    languageMatch: 'ANY',
    joiningFrom: '',
    joiningTo: '',
    sortBy: '',
    sortOrder: 'ASC',
  })

  // Applications filters
  const [applicationsFilters, setApplicationsFilters] = useState({
    programId: '',
    statuses: [],
  })

  const loadSewadars = async (page = 0) => {
    setLoading(true)
    try {
      const languages = sewadarsFilters.languages
        ? sewadarsFilters.languages.split(',').map((l) => l.trim()).filter((l) => l)
        : null

      const response = await api.post('/dashboard/sewadars', {
        page,
        size: sewadarsRowsPerPage,
        location: sewadarsFilters.location || null,
        languages,
        languageMatchType: sewadarsFilters.languageMatch,
        joiningDateFrom: sewadarsFilters.joiningFrom || null,
        joiningDateTo: sewadarsFilters.joiningTo || null,
        sortBy: sewadarsFilters.sortBy || null,
        sortOrder: sewadarsFilters.sortOrder,
      })

      setSewadars(response.data.sewadars || [])
      setTotalSewadars(response.data.totalElements || 0)
    } catch (error) {
      console.error('Error loading sewadars:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadApplications = async (page = 0) => {
    setLoading(true)
    try {
      const response = await api.post('/dashboard/applications', {
        page,
        size: applicationsRowsPerPage,
        programId: applicationsFilters.programId ? parseInt(applicationsFilters.programId) : null,
        statuses: applicationsFilters.statuses.length > 0 ? applicationsFilters.statuses : null,
      })

      setApplications(response.data.applications || [])
      setTotalApplications(response.data.totalElements || 0)
    } catch (error) {
      console.error('Error loading applications:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleExport = async (type, format) => {
    try {
      let url = ''
      if (type === 'sewadars') {
        url = `/dashboard/sewadars/export/${format}`
        const languages = sewadarsFilters.languages
          ? sewadarsFilters.languages.split(',').map((l) => l.trim()).filter((l) => l)
          : null

        const response = await api.post(
          url,
          {
            page: 0,
            size: 10000,
            location: sewadarsFilters.location || null,
            languages,
            languageMatchType: sewadarsFilters.languageMatch,
            joiningDateFrom: sewadarsFilters.joiningFrom || null,
            joiningDateTo: sewadarsFilters.joiningTo || null,
            sortBy: sewadarsFilters.sortBy || null,
            sortOrder: sewadarsFilters.sortOrder,
          },
          { responseType: 'blob' }
        )

        const blob = new Blob([response.data])
        const downloadUrl = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = downloadUrl
        link.download = `sewadars.${format.toLowerCase()}`
        link.click()
      } else if (type === 'applications') {
        url = `/dashboard/applications/export/${format}`
        const response = await api.post(
          url,
          {
            page: 0,
            size: 10000,
            programId: applicationsFilters.programId ? parseInt(applicationsFilters.programId) : null,
            statuses: applicationsFilters.statuses.length > 0 ? applicationsFilters.statuses : null,
          },
          { responseType: 'blob' }
        )

        const blob = new Blob([response.data])
        const downloadUrl = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = downloadUrl
        link.download = `applications.${format.toLowerCase()}`
        link.click()
      }
    } catch (error) {
      alert('Export failed: ' + (error.response?.data?.message || error.message))
    }
  }

  return (
    <Box>
      <Typography variant="h4" component="h1" sx={{ fontWeight: 600, mb: 3 }}>
        Dashboard
      </Typography>

      <Paper sx={{ mb: 3 }}>
        <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)}>
          <Tab label="Sewadars" />
          <Tab label="Applications" />
        </Tabs>
      </Paper>

      {activeTab === 0 && (
        <Box>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Filters & Sorting
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6} md={3}>
                <TextField
                  fullWidth
                  label="Location"
                  value={sewadarsFilters.location}
                  onChange={(e) =>
                    setSewadarsFilters({ ...sewadarsFilters, location: e.target.value })
                  }
                  size="small"
                />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <TextField
                  fullWidth
                  label="Languages"
                  value={sewadarsFilters.languages}
                  onChange={(e) =>
                    setSewadarsFilters({ ...sewadarsFilters, languages: e.target.value })
                  }
                  size="small"
                  placeholder="e.g., Hindi, English"
                />
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <FormControl fullWidth size="small">
                  <InputLabel>Match Type</InputLabel>
                  <Select
                    value={sewadarsFilters.languageMatch}
                    onChange={(e) =>
                      setSewadarsFilters({
                        ...sewadarsFilters,
                        languageMatch: e.target.value,
                      })
                    }
                    label="Match Type"
                  >
                    <MenuItem value="ANY">Any</MenuItem>
                    <MenuItem value="ALL">All</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <TextField
                  fullWidth
                  type="date"
                  label="Joining From"
                  value={sewadarsFilters.joiningFrom}
                  onChange={(e) =>
                    setSewadarsFilters({ ...sewadarsFilters, joiningFrom: e.target.value })
                  }
                  size="small"
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <TextField
                  fullWidth
                  type="date"
                  label="Joining To"
                  value={sewadarsFilters.joiningTo}
                  onChange={(e) =>
                    setSewadarsFilters({ ...sewadarsFilters, joiningTo: e.target.value })
                  }
                  size="small"
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <FormControl fullWidth size="small">
                  <InputLabel>Sort By</InputLabel>
                  <Select
                    value={sewadarsFilters.sortBy}
                    onChange={(e) =>
                      setSewadarsFilters({ ...sewadarsFilters, sortBy: e.target.value })
                    }
                    label="Sort By"
                  >
                    <MenuItem value="">Default</MenuItem>
                    <MenuItem value="totalPrograms">Total Programs</MenuItem>
                    <MenuItem value="totalDays">Total Days</MenuItem>
                    <MenuItem value="beasDays">BEAS Days</MenuItem>
                    <MenuItem value="nonBeasDays">Non-BEAS Days</MenuItem>
                    <MenuItem value="joiningDate">Joining Date</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <FormControl fullWidth size="small">
                  <InputLabel>Order</InputLabel>
                  <Select
                    value={sewadarsFilters.sortOrder}
                    onChange={(e) =>
                      setSewadarsFilters({ ...sewadarsFilters, sortOrder: e.target.value })
                    }
                    label="Order"
                  >
                    <MenuItem value="ASC">Ascending</MenuItem>
                    <MenuItem value="DESC">Descending</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <Box display="flex" gap={2}>
                  <Button variant="contained" onClick={() => loadSewadars(0)}>
                    Apply Filters
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={() => {
                      setSewadarsFilters({
                        location: '',
                        languages: '',
                        languageMatch: 'ANY',
                        joiningFrom: '',
                        joiningTo: '',
                        sortBy: '',
                        sortOrder: 'ASC',
                      })
                      loadSewadars(0)
                    }}
                  >
                    Clear
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<DownloadIcon />}
                    onClick={() => handleExport('sewadars', 'CSV')}
                  >
                    Export CSV
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<DownloadIcon />}
                    onClick={() => handleExport('sewadars', 'XLSX')}
                  >
                    Export XLSX
                  </Button>
                </Box>
              </Grid>
            </Grid>
          </Paper>

          {loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Zonal ID</TableCell>
                    <TableCell>Name</TableCell>
                    <TableCell>Mobile</TableCell>
                    <TableCell>Location</TableCell>
                    <TableCell>Total Programs</TableCell>
                    <TableCell>Total Days</TableCell>
                    <TableCell>BEAS Programs</TableCell>
                    <TableCell>BEAS Days</TableCell>
                    <TableCell>Non-BEAS Programs</TableCell>
                    <TableCell>Non-BEAS Days</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {sewadars.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={10} align="center">
                        <Alert severity="info">Click "Apply Filters" to load sewadars</Alert>
                      </TableCell>
                    </TableRow>
                  ) : (
                    sewadars.map((sewadar) => (
                      <TableRow key={sewadar.zonalId}>
                        <TableCell>{sewadar.zonalId}</TableCell>
                        <TableCell>
                          {sewadar.firstName} {sewadar.lastName}
                        </TableCell>
                        <TableCell>{sewadar.mobile || ''}</TableCell>
                        <TableCell>{sewadar.location || ''}</TableCell>
                        <TableCell>{sewadar.totalProgramsCount || 0}</TableCell>
                        <TableCell>{sewadar.totalDaysAttended || 0}</TableCell>
                        <TableCell>{sewadar.beasProgramsCount || 0}</TableCell>
                        <TableCell>{sewadar.beasDaysAttended || 0}</TableCell>
                        <TableCell>{sewadar.nonBeasProgramsCount || 0}</TableCell>
                        <TableCell>{sewadar.nonBeasDaysAttended || 0}</TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
              <TablePagination
                component="div"
                count={totalSewadars}
                page={sewadarsPage}
                onPageChange={(e, newPage) => {
                  setSewadarsPage(newPage)
                  loadSewadars(newPage)
                }}
                rowsPerPage={sewadarsRowsPerPage}
                onRowsPerPageChange={(e) => {
                  setSewadarsRowsPerPage(parseInt(e.target.value, 10))
                  setSewadarsPage(0)
                  loadSewadars(0)
                }}
                rowsPerPageOptions={[10, 25, 50, 100]}
              />
            </TableContainer>
          )}
        </Box>
      )}

      {activeTab === 1 && (
        <Box>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Application Filters
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6} md={3}>
                <TextField
                  fullWidth
                  label="Program ID"
                  type="number"
                  value={applicationsFilters.programId}
                  onChange={(e) =>
                    setApplicationsFilters({
                      ...applicationsFilters,
                      programId: e.target.value,
                    })
                  }
                  size="small"
                />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <FormControl fullWidth size="small">
                  <InputLabel>Status</InputLabel>
                  <Select
                    multiple
                    value={applicationsFilters.statuses}
                    onChange={(e) =>
                      setApplicationsFilters({
                        ...applicationsFilters,
                        statuses: e.target.value,
                      })
                    }
                    label="Status"
                  >
                    <MenuItem value="PENDING">PENDING</MenuItem>
                    <MenuItem value="APPROVED">APPROVED</MenuItem>
                    <MenuItem value="REJECTED">REJECTED</MenuItem>
                    <MenuItem value="DROPPED">DROPPED</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <Box display="flex" gap={2}>
                  <Button variant="contained" onClick={() => loadApplications(0)}>
                    Apply Filters
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={() => {
                      setApplicationsFilters({ programId: '', statuses: [] })
                      loadApplications(0)
                    }}
                  >
                    Clear
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<DownloadIcon />}
                    onClick={() => handleExport('applications', 'CSV')}
                  >
                    Export CSV
                  </Button>
                </Box>
              </Grid>
            </Grid>
          </Paper>

          {loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Application ID</TableCell>
                    <TableCell>Zonal ID</TableCell>
                    <TableCell>Name</TableCell>
                    <TableCell>Mobile</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Applied At</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {applications.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        <Alert severity="info">Click "Apply Filters" to load applications</Alert>
                      </TableCell>
                    </TableRow>
                  ) : (
                    applications.map((app) => (
                      <TableRow key={app.applicationId}>
                        <TableCell>{app.applicationId}</TableCell>
                        <TableCell>{app.sewadarZonalId}</TableCell>
                        <TableCell>{app.sewadarName}</TableCell>
                        <TableCell>{app.mobile || ''}</TableCell>
                        <TableCell>{app.status}</TableCell>
                        <TableCell>{app.appliedAt || ''}</TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
              <TablePagination
                component="div"
                count={totalApplications}
                page={applicationsPage}
                onPageChange={(e, newPage) => {
                  setApplicationsPage(newPage)
                  loadApplications(newPage)
                }}
                rowsPerPage={applicationsRowsPerPage}
                onRowsPerPageChange={(e) => {
                  setApplicationsRowsPerPage(parseInt(e.target.value, 10))
                  setApplicationsPage(0)
                  loadApplications(0)
                }}
                rowsPerPageOptions={[10, 25, 50, 100]}
              />
            </TableContainer>
          )}
        </Box>
      )}
    </Box>
  )
}

export default Dashboard

