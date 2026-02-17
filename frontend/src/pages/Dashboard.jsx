import { useState, useEffect } from 'react'
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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Chip,
  Autocomplete,
} from '@mui/material'
import {
  Search as SearchIcon,
  Download as DownloadIcon,
  Visibility as VisibilityIcon,
  Edit as EditIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material'
import { format } from 'date-fns'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const Dashboard = () => {
  const { user } = useAuth()
  const [activeTab, setActiveTab] = useState(0)
  const [sewadars, setSewadars] = useState([])
  const [loading, setLoading] = useState(false)
  const [sewadarsPage, setSewadarsPage] = useState(0)
  const [sewadarsRowsPerPage, setSewadarsRowsPerPage] = useState(25)
  const [totalSewadars, setTotalSewadars] = useState(0)
  const [selectedSewadarForAttendance, setSelectedSewadarForAttendance] = useState(null)
  const [sewadarAttendanceDetails, setSewadarAttendanceDetails] = useState(null)
  const [openSewadarAttendanceDialog, setOpenSewadarAttendanceDialog] = useState(false)

  // Attendance Lookup states
  const [programs, setPrograms] = useState([])
  const [allSewadars, setAllSewadars] = useState([])
  const [selectedProgramForLookup, setSelectedProgramForLookup] = useState(null)
  const [selectedSewadarForLookup, setSelectedSewadarForLookup] = useState(null)
  const [lookupDate, setLookupDate] = useState('')
  const [lookupResult, setLookupResult] = useState(null)
  const [selectedProgramForDownload, setSelectedProgramForDownload] = useState(null)
  const [selectedProgramsForMulti, setSelectedProgramsForMulti] = useState([])

  // Form Submissions states
  const [formSubmissions, setFormSubmissions] = useState([])
  const [selectedProgramForForms, setSelectedProgramForForms] = useState(null)
  const [openFormEditDialog, setOpenFormEditDialog] = useState(false)
  const [editingForm, setEditingForm] = useState(null)
  const [formEditData, setFormEditData] = useState({})

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

  useEffect(() => {
    if (activeTab === 1 || activeTab === 2) {
      // Load programs for attendance lookup and form submissions
      loadPrograms()
    }
    if (activeTab === 1) {
      // Load sewadars for quick lookup autocomplete
      loadAllSewadars()
    }
  }, [activeTab])

  const loadPrograms = async () => {
    try {
      const response = await api.get('/programs')
      setPrograms(response.data)
    } catch (error) {
      console.error('Error loading programs:', error)
    }
  }

  const loadAllSewadars = async () => {
    try {
      const response = await api.get('/sewadars')
      setAllSewadars(response.data)
    } catch (error) {
      console.error('Error loading sewadars:', error)
    }
  }

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

  const handleViewSewadarAttendance = async (sewadarId) => {
    try {
      setLoading(true)
      const response = await api.get(`/dashboard/sewadar/${sewadarId}/attendance`)
      setSewadarAttendanceDetails(response.data)
      setSelectedSewadarForAttendance(sewadarId)
      setOpenSewadarAttendanceDialog(true)
    } catch (error) {
      console.error('Error loading sewadar attendance:', error)
      alert('Error loading attendance details: ' + (error.response?.data?.message || error.message))
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
        const url_blob = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url_blob
        link.download = `sewadars_${format.toLowerCase()}.${format.toLowerCase()}`
        link.click()
        window.URL.revokeObjectURL(url_blob)
      }
    } catch (error) {
      console.error('Error exporting:', error)
      alert('Error exporting data: ' + (error.response?.data?.message || error.message))
    }
  }

  // Attendance Lookup Functions
  const handleQuickLookup = async () => {
    if (!selectedSewadarForLookup || !selectedProgramForLookup || !lookupDate) {
      alert('Please select sewadar, program, and date')
      return
    }

    try {
      setLoading(true)
      const response = await api.get('/attendances/lookup', {
        params: {
          sewadarId: selectedSewadarForLookup,
          programId: selectedProgramForLookup.id,
          date: lookupDate,
        },
      })
      setLookupResult(response.data)
    } catch (error) {
      console.error('Error checking attendance:', error)
      alert('Error: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  const handleDownloadProgramAttendance = async () => {
    if (!selectedProgramForDownload) {
      alert('Please select a program')
      return
    }

    try {
      setLoading(true)
      const response = await api.get(`/dashboard/program/${selectedProgramForDownload.id}/attendance/export/CSV`, {
        responseType: 'blob',
      })

      const blob = new Blob([response.data])
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `attendance_${selectedProgramForDownload.title.replace(/\s+/g, '_')}_${selectedProgramForDownload.id}.csv`
      link.click()
      window.URL.revokeObjectURL(url)
      alert('Attendance CSV downloaded successfully!')
    } catch (error) {
      console.error('Error downloading attendance:', error)
      alert('Error: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  const generateAttendanceCSV = (attendanceRecords) => {
    const headers = ['Program', 'Program ID', 'Sewadar Zonal ID', 'Sewadar Name', 'Date', 'Status']
    const rows = attendanceRecords.map(record => {
      const programTitle = record.programTitle || `Program ${record.programId || ''}`
      const sewadarName = record.sewadar 
        ? `${record.sewadar.firstName || ''} ${record.sewadar.lastName || ''}`.trim()
        : ''
      const sewadarZonalId = record.sewadar?.zonalId || ''
      const date = record.attendanceDate 
        ? format(new Date(record.attendanceDate), 'yyyy-MM-dd')
        : ''
      const status = record.attended !== false ? 'Present' : 'Absent'
      
      return [
        programTitle,
        record.programId || '',
        sewadarZonalId,
        sewadarName,
        date,
        status
      ]
    })

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => {
        // Escape commas and quotes in CSV
        const cellStr = String(cell || '')
        if (cellStr.includes(',') || cellStr.includes('"') || cellStr.includes('\n')) {
          return `"${cellStr.replace(/"/g, '""')}"`
        }
        return cellStr
      }).join(','))
    ].join('\n')

    return csvContent
  }

  const downloadCSV = (csvContent, filename) => {
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    window.URL.revokeObjectURL(url)
  }

  const handleDownloadMultiProgramAttendance = async () => {
    if (selectedProgramsForMulti.length === 0) {
      alert('Please select at least one program')
      return
    }

    try {
      setLoading(true)
      const programIds = selectedProgramsForMulti.map(p => p.id)
      // Load attendance for each program and combine
      const allAttendance = []
      for (const programId of programIds) {
        try {
          const response = await api.get(`/attendances/program/${programId}`)
          // Add program title to each record
          const program = programs.find(p => p.id === programId)
          const recordsWithProgram = response.data.map(record => ({
            ...record,
            programTitle: program?.title || `Program ${programId}`,
            programId: programId,
          }))
          allAttendance.push(...recordsWithProgram)
        } catch (error) {
          console.error(`Error loading attendance for program ${programId}:`, error)
        }
      }

      if (allAttendance.length === 0) {
        alert('No attendance records found for selected programs')
        return
      }

      const csvContent = generateAttendanceCSV(allAttendance)
      const programTitles = selectedProgramsForMulti.map(p => p.title.replace(/\s+/g, '_')).join('_')
      const filename = `multi_program_attendance_${programTitles}_${Date.now()}.csv`
      downloadCSV(csvContent, filename)
      alert('Multi-program attendance CSV downloaded successfully!')
    } catch (error) {
      console.error('Error downloading multi-program attendance:', error)
      alert('Error: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  const handleDownloadAllProgramsAttendance = async () => {
    if (programs.length === 0) {
      alert('No programs available')
      return
    }

    try {
      setLoading(true)
      // Load attendance for all programs
      const allAttendance = []
      for (const program of programs) {
        try {
          const response = await api.get(`/attendances/program/${program.id}`)
          const recordsWithProgram = response.data.map(record => ({
            ...record,
            programTitle: program.title,
            programId: program.id,
          }))
          allAttendance.push(...recordsWithProgram)
        } catch (error) {
          console.error(`Error loading attendance for program ${program.id}:`, error)
        }
      }

      if (allAttendance.length === 0) {
        alert('No attendance records found')
        return
      }

      const csvContent = generateAttendanceCSV(allAttendance)
      const filename = `all_programs_attendance_${Date.now()}.csv`
      downloadCSV(csvContent, filename)
      alert('All programs attendance CSV downloaded successfully!')
    } catch (error) {
      console.error('Error downloading all programs attendance:', error)
      alert('Error: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  // Form Submissions Functions
  const handleLoadFormSubmissions = async () => {
    if (!selectedProgramForForms) {
      alert('Please select a program')
      return
    }

    try {
      setLoading(true)
      const response = await api.get(`/form-submissions/program/${selectedProgramForForms.id}`)
      setFormSubmissions(response.data)
    } catch (error) {
      console.error('Error loading form submissions:', error)
      alert('Error: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  const handleExportFormSubmissions = async () => {
    if (!selectedProgramForForms) {
      alert('Please select a program')
      return
    }

    try {
      setLoading(true)
      const response = await api.get(`/form-submissions/program/${selectedProgramForForms.id}/export/csv`, {
        responseType: 'blob',
      })

        const blob = new Blob([response.data])
      const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
      link.href = url
      link.download = `form_submissions_${selectedProgramForForms.title.replace(/\s+/g, '_')}_${selectedProgramForForms.id}.csv`
        link.click()
      window.URL.revokeObjectURL(url)
      alert('Form submissions CSV downloaded successfully!')
    } catch (error) {
      console.error('Error exporting form submissions:', error)
      alert('Error: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  const handleEditForm = (form) => {
    setEditingForm(form)
    setFormEditData({
      programId: form.programId,
      startingDateTimeFromHome: form.startingDateTimeFromHome || '',
      reachingDateTimeToHome: form.reachingDateTimeToHome || '',
      onwardTrainFlightDateTime: form.onwardTrainFlightDateTime || '',
      onwardTrainFlightNo: form.onwardTrainFlightNo || '',
      returnTrainFlightDateTime: form.returnTrainFlightDateTime || '',
      returnTrainFlightNo: form.returnTrainFlightNo || '',
      stayInHotel: form.stayInHotel || '',
      stayInPandal: form.stayInPandal || '',
    })
    setOpenFormEditDialog(true)
  }

  const handleSaveFormEdit = async () => {
    try {
      setLoading(true)
      await api.put(`/form-submissions/${editingForm.id}`, formEditData)
      alert('Form submission updated successfully!')
      setOpenFormEditDialog(false)
      handleLoadFormSubmissions()
    } catch (error) {
      console.error('Error updating form submission:', error)
      alert('Error: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
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
          <Tab label="Attendance Lookup" />
          <Tab label="Form Submissions" />
        </Tabs>
      </Paper>

      {/* Tab 0: Sewadars */}
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
                  <Button
                    variant="outlined"
                    startIcon={<DownloadIcon />}
                    onClick={() => handleExport('sewadars', 'PDF')}
                  >
                    Export PDF
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
                    <TableCell>Profession</TableCell>
                    <TableCell>Joining Date</TableCell>
                    <TableCell>Languages</TableCell>
                    <TableCell>Total Programs</TableCell>
                    <TableCell>Total Days</TableCell>
                    <TableCell>BEAS Programs</TableCell>
                    <TableCell>BEAS Days</TableCell>
                    <TableCell>Non-BEAS Programs</TableCell>
                    <TableCell>Non-BEAS Days</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {sewadars.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={14} align="center">
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
                        <TableCell>{sewadar.profession || '-'}</TableCell>
                        <TableCell>
                          {sewadar.joiningDate ? format(new Date(sewadar.joiningDate), 'MMM dd, yyyy') : '-'}
                        </TableCell>
                        <TableCell>
                          {sewadar.languages && sewadar.languages.length > 0
                            ? sewadar.languages.join(', ')
                            : '-'}
                        </TableCell>
                        <TableCell>{sewadar.totalProgramsCount || 0}</TableCell>
                        <TableCell>{sewadar.totalDaysAttended || 0}</TableCell>
                        <TableCell>{sewadar.beasProgramsCount || 0}</TableCell>
                        <TableCell>{sewadar.beasDaysAttended || 0}</TableCell>
                        <TableCell>{sewadar.nonBeasProgramsCount || 0}</TableCell>
                        <TableCell>{sewadar.nonBeasDaysAttended || 0}</TableCell>
                        <TableCell>
                          <Button
                            size="small"
                            variant="outlined"
                            startIcon={<VisibilityIcon />}
                            onClick={() => handleViewSewadarAttendance(sewadar.zonalId)}
                          >
                            View Details
                          </Button>
                        </TableCell>
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

      {/* Tab 1: Attendance Lookup */}
      {activeTab === 1 && (
        <Box>
          <Grid container spacing={3}>
            {/* Quick Lookup Card */}
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
            <Typography variant="h6" gutterBottom>
                    Quick Lookup
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Check if a sewadar is present on a specific date for a program
            </Typography>
            <Grid container spacing={2}>
                    <Grid item xs={12}>
                      <Autocomplete
                        options={programs}
                        getOptionLabel={(option) => option.title || ''}
                        value={selectedProgramForLookup}
                        onChange={(e, newValue) => setSelectedProgramForLookup(newValue)}
                        renderInput={(params) => (
                          <TextField {...params} label="Select Program" size="small" />
                        )}
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <Autocomplete
                        options={allSewadars}
                        getOptionLabel={(option) => `${option.zonalId} - ${option.firstName} ${option.lastName}`}
                        value={allSewadars.find(s => s.zonalId === selectedSewadarForLookup) || null}
                        onChange={(e, newValue) => setSelectedSewadarForLookup(newValue?.zonalId || '')}
                        renderInput={(params) => (
                          <TextField {...params} label="Select Sewadar" size="small" />
                        )}
                      />
                    </Grid>
                    <Grid item xs={12}>
                <TextField
                  fullWidth
                        type="date"
                        label="Date"
                        value={lookupDate}
                        onChange={(e) => setLookupDate(e.target.value)}
                  size="small"
                        InputLabelProps={{ shrink: true }}
                />
              </Grid>
                    <Grid item xs={12}>
                      <Button
                        variant="contained"
                        fullWidth
                        onClick={handleQuickLookup}
                        disabled={loading || !selectedProgramForLookup || !selectedSewadarForLookup || !lookupDate}
                      >
                        Check Attendance
                      </Button>
              </Grid>
                    {lookupResult && (
              <Grid item xs={12}>
                        <Alert
                          severity={lookupResult.isPresent ? 'success' : 'info'}
                          icon={lookupResult.isPresent ? <CheckCircleIcon /> : <CancelIcon />}
                        >
                          {lookupResult.isPresent
                            ? `✅ Present on ${format(new Date(lookupResult.date), 'MMM dd, yyyy')}`
                            : `❌ Not present on ${format(new Date(lookupResult.date), 'MMM dd, yyyy')}`}
                        </Alert>
                      </Grid>
                    )}
                  </Grid>
                </CardContent>
              </Card>
            </Grid>

            {/* Program Attendance Download Card */}
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Download Program Attendance
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Download attendance matrix (CSV) for a specific program
                  </Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={12}>
                      <Autocomplete
                        options={programs}
                        getOptionLabel={(option) => option.title || ''}
                        value={selectedProgramForDownload}
                        onChange={(e, newValue) => {
                          setSelectedProgramForDownload(newValue)
                        }}
                        renderInput={(params) => (
                          <TextField {...params} label="Select Program" size="small" />
                        )}
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <Button
                        variant="contained"
                        fullWidth
                        startIcon={<DownloadIcon />}
                        onClick={handleDownloadProgramAttendance}
                        disabled={loading || !selectedProgramForDownload}
                      >
                        Download Attendance CSV
                      </Button>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            </Grid>

            {/* Multi-Program Attendance Card */}
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Multi-Program Attendance
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Download attendance records (present/absent) across multiple programs as CSV
                  </Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={12}>
                      <Autocomplete
                        multiple
                        options={programs}
                        getOptionLabel={(option) => option.title || ''}
                        value={selectedProgramsForMulti}
                        onChange={(e, newValue) => {
                          setSelectedProgramsForMulti(newValue)
                        }}
                        renderInput={(params) => (
                          <TextField {...params} label="Select Programs" size="small" />
                        )}
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <Button
                        variant="contained"
                        fullWidth
                        startIcon={<DownloadIcon />}
                        onClick={handleDownloadMultiProgramAttendance}
                        disabled={loading || selectedProgramsForMulti.length === 0}
                      >
                        Download Attendance CSV
                      </Button>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            </Grid>

            {/* All Programs Attendance Card */}
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    All Programs Attendance
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Download attendance records (present/absent) for all programs as CSV
                  </Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={12}>
                      <Button
                        variant="contained"
                        fullWidth
                        startIcon={<DownloadIcon />}
                        onClick={handleDownloadAllProgramsAttendance}
                        disabled={loading || programs.length === 0}
                      >
                        Download All Programs Attendance CSV
                      </Button>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Box>
      )}

      {/* Tab 2: Form Submissions */}
      {activeTab === 2 && (
        <Box>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Form Submissions
            </Typography>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={8}>
                <Autocomplete
                  options={programs}
                  getOptionLabel={(option) => option.title || ''}
                  value={selectedProgramForForms}
                  onChange={(e, newValue) => {
                    setSelectedProgramForForms(newValue)
                    setFormSubmissions([])
                  }}
                  renderInput={(params) => (
                    <TextField {...params} label="Select Program" size="small" />
                  )}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Box display="flex" gap={1}>
                  <Button
                    variant="contained"
                    onClick={handleLoadFormSubmissions}
                    disabled={loading || !selectedProgramForForms}
                    fullWidth
                  >
                    Load Forms
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<DownloadIcon />}
                    onClick={handleExportFormSubmissions}
                    disabled={loading || !selectedProgramForForms || formSubmissions.length === 0}
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
          ) : formSubmissions.length === 0 ? (
            <Alert severity="info">
              {selectedProgramForForms
                ? 'No form submissions found for this program. Click "Load Forms" to refresh.'
                : 'Please select a program to view form submissions.'}
            </Alert>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Sewadar Zonal ID</TableCell>
                    <TableCell>Sewadar Name</TableCell>
                    <TableCell>Starting From Home</TableCell>
                    <TableCell>Reaching To Home</TableCell>
                    <TableCell>Onward Train/Flight</TableCell>
                    <TableCell>Return Train/Flight</TableCell>
                    <TableCell>Stay In Hotel</TableCell>
                    <TableCell>Stay In Pandal</TableCell>
                    <TableCell>Submitted At</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {formSubmissions.map((form) => (
                    <TableRow key={form.id}>
                      <TableCell>{form.sewadarId}</TableCell>
                      <TableCell>{form.sewadarName}</TableCell>
                      <TableCell>
                        {form.startingDateTimeFromHome
                          ? format(new Date(form.startingDateTimeFromHome), 'MMM dd, yyyy HH:mm')
                          : '-'}
                      </TableCell>
                        <TableCell>
                        {form.reachingDateTimeToHome
                          ? format(new Date(form.reachingDateTimeToHome), 'MMM dd, yyyy HH:mm')
                          : '-'}
                        </TableCell>
                        <TableCell>
                        {form.onwardTrainFlightNo
                          ? `${form.onwardTrainFlightNo} (${form.onwardTrainFlightDateTime ? format(new Date(form.onwardTrainFlightDateTime), 'MMM dd, yyyy HH:mm') : ''})`
                          : '-'}
                      </TableCell>
                      <TableCell>
                        {form.returnTrainFlightNo
                          ? `${form.returnTrainFlightNo} (${form.returnTrainFlightDateTime ? format(new Date(form.returnTrainFlightDateTime), 'MMM dd, yyyy HH:mm') : ''})`
                          : '-'}
                      </TableCell>
                      <TableCell>{form.stayInHotel || '-'}</TableCell>
                      <TableCell>{form.stayInPandal || '-'}</TableCell>
                      <TableCell>
                        {form.submittedAt
                          ? format(new Date(form.submittedAt), 'MMM dd, yyyy HH:mm')
                          : '-'}
                      </TableCell>
                      <TableCell>
                        <Button
                          size="small"
                          variant="outlined"
                          startIcon={<EditIcon />}
                          onClick={() => handleEditForm(form)}
                        >
                          Edit
                        </Button>
                        </TableCell>
                      </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Box>
      )}

      {/* Sewadar Attendance Details Dialog */}
      <Dialog
        open={openSewadarAttendanceDialog}
        onClose={() => setOpenSewadarAttendanceDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          Attendance Details - {selectedSewadarForAttendance}
        </DialogTitle>
        <DialogContent>
          {sewadarAttendanceDetails ? (
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                <strong>Total Programs:</strong> {sewadarAttendanceDetails.totalProgramsCount || 0}
              </Typography>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                <strong>Total Days:</strong> {sewadarAttendanceDetails.totalDaysAttended || 0}
                </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                <strong>BEAS Programs:</strong> {sewadarAttendanceDetails.beasProgramsCount || 0} |{' '}
                <strong>BEAS Days:</strong> {sewadarAttendanceDetails.beasDaysAttended || 0}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                <strong>Non-BEAS Programs:</strong> {sewadarAttendanceDetails.nonBeasProgramsCount || 0} |{' '}
                <strong>Non-BEAS Days:</strong> {sewadarAttendanceDetails.nonBeasDaysAttended || 0}
              </Typography>
              {sewadarAttendanceDetails.attendanceDetails && sewadarAttendanceDetails.attendanceDetails.length > 0 && (
              <TableContainer sx={{ mt: 2 }}>
                  <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Program</TableCell>
                      <TableCell>Location</TableCell>
                      <TableCell>Date</TableCell>
                      <TableCell>Status</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                      {sewadarAttendanceDetails.attendanceDetails.map((detail, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{detail.programTitle}</TableCell>
                          <TableCell>{detail.location}</TableCell>
                          <TableCell>
                            {detail.markedAt
                              ? format(new Date(detail.markedAt), 'MMM dd, yyyy')
                              : '-'}
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={detail.attended ? 'Present' : 'Absent'}
                              color={detail.attended ? 'success' : 'default'}
                              size="small"
                            />
                          </TableCell>
                        </TableRow>
                      ))}
                  </TableBody>
                </Table>
              </TableContainer>
              )}
            </Box>
          ) : (
            <CircularProgress />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenSewadarAttendanceDialog(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Form Edit Dialog */}
      <Dialog
        open={openFormEditDialog}
        onClose={() => setOpenFormEditDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Edit Form Submission</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Starting Date/Time From Home"
                value={formEditData.startingDateTimeFromHome ? new Date(formEditData.startingDateTimeFromHome).toISOString().slice(0, 16) : ''}
                onChange={(e) =>
                  setFormEditData({
                    ...formEditData,
                    startingDateTimeFromHome: e.target.value ? new Date(e.target.value).toISOString() : null,
                  })
                }
                InputLabelProps={{ shrink: true }}
                size="small"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Reaching Date/Time To Home"
                value={formEditData.reachingDateTimeToHome ? new Date(formEditData.reachingDateTimeToHome).toISOString().slice(0, 16) : ''}
                onChange={(e) =>
                  setFormEditData({
                    ...formEditData,
                    reachingDateTimeToHome: e.target.value ? new Date(e.target.value).toISOString() : null,
                  })
                }
                InputLabelProps={{ shrink: true }}
                size="small"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Onward Train/Flight Date/Time"
                value={formEditData.onwardTrainFlightDateTime ? new Date(formEditData.onwardTrainFlightDateTime).toISOString().slice(0, 16) : ''}
                onChange={(e) =>
                  setFormEditData({
                    ...formEditData,
                    onwardTrainFlightDateTime: e.target.value ? new Date(e.target.value).toISOString() : null,
                  })
                }
                InputLabelProps={{ shrink: true }}
                size="small"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Onward Train/Flight No"
                value={formEditData.onwardTrainFlightNo || ''}
                onChange={(e) =>
                  setFormEditData({ ...formEditData, onwardTrainFlightNo: e.target.value })
                }
                size="small"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Return Train/Flight Date/Time"
                value={formEditData.returnTrainFlightDateTime ? new Date(formEditData.returnTrainFlightDateTime).toISOString().slice(0, 16) : ''}
                onChange={(e) =>
                  setFormEditData({
                    ...formEditData,
                    returnTrainFlightDateTime: e.target.value ? new Date(e.target.value).toISOString() : null,
                  })
                }
                InputLabelProps={{ shrink: true }}
                size="small"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Return Train/Flight No"
                value={formEditData.returnTrainFlightNo || ''}
                onChange={(e) =>
                  setFormEditData({ ...formEditData, returnTrainFlightNo: e.target.value })
                }
                size="small"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Stay In Hotel"
                value={formEditData.stayInHotel || ''}
                onChange={(e) =>
                  setFormEditData({ ...formEditData, stayInHotel: e.target.value })
                }
                multiline
                rows={2}
                size="small"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Stay In Pandal"
                value={formEditData.stayInPandal || ''}
                onChange={(e) =>
                  setFormEditData({ ...formEditData, stayInPandal: e.target.value })
                }
                multiline
                rows={2}
                size="small"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenFormEditDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveFormEdit} disabled={loading}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default Dashboard
