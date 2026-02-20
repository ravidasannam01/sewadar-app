import { useState, useEffect } from 'react'
import {
  TextField,
  Button,
  Box,
  MenuItem,
  IconButton,
  Typography,
} from '@mui/material'
import { Add as AddIcon, Delete as DeleteIcon } from '@mui/icons-material'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const ProgramForm = ({ program, onClose, onSuccess }) => {
  const { user } = useAuth()
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    location: '',
    status: 'scheduled',
    maxSewadars: '',
    lastDateToApply: '',
    lastDateToSubmitForm: '',
    programDates: [{ date: '' }],
  })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (program) {
      setFormData({
        title: program.title || '',
        description: program.description || '',
        location: program.location || '',
        status: program.status || 'scheduled',
        maxSewadars: program.maxSewadars || '',
        lastDateToApply: program.lastDateToApply ? String(program.lastDateToApply).slice(0, 16) : '',
        lastDateToSubmitForm: program.lastDateToSubmitForm ? String(program.lastDateToSubmitForm).slice(0, 16) : '',
        programDates: program.programDates?.length > 0
          ? program.programDates.map((d) => ({ date: d }))
          : [{ date: '' }],
      })
    }
  }, [program])

  const handleChange = (field, value) => {
    if (field === 'maxSewadars' && value !== '') {
      const num = parseInt(value, 10)
      if (isNaN(num) || num < 1) return
    }
    setFormData((prev) => ({ ...prev, [field]: value }))
  }

  const handleDateChange = (index, value) => {
    const newDates = [...formData.programDates]
    newDates[index].date = value
    setFormData((prev) => ({ ...prev, programDates: newDates }))
  }

  const addDate = () => {
    setFormData((prev) => ({
      ...prev,
      programDates: [...prev.programDates, { date: '' }],
    }))
  }

  const removeDate = (index) => {
    const newDates = formData.programDates.filter((_, i) => i !== index)
    setFormData((prev) => ({ ...prev, programDates: newDates }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)

    const dates = formData.programDates
      .map((d) => d.date)
      .filter((d) => d)

    if (dates.length === 0) {
      alert('Please add at least one program date')
      setLoading(false)
      return
    }

    const maxSew = formData.maxSewadars ? parseInt(formData.maxSewadars, 10) : null
    if (maxSew !== null && (isNaN(maxSew) || maxSew < 1)) {
      alert('Max Sewadars must be a positive number (min: 1)')
      setLoading(false)
      return
    }

    try {
      const toIsoWithSeconds = (dtLocal) => {
        if (!dtLocal) return null
        // datetime-local gives "YYYY-MM-DDTHH:mm" -> backend LocalDateTime prefers seconds
        return dtLocal.length === 16 ? `${dtLocal}:00` : dtLocal
      }

      const payload = {
        title: formData.title,
        description: formData.description,
        location: formData.location,
        status: formData.status,
        programDates: dates,
        maxSewadars: formData.maxSewadars ? parseInt(formData.maxSewadars) : null,
        lastDateToApply: toIsoWithSeconds(formData.lastDateToApply),
        lastDateToSubmitForm: toIsoWithSeconds(formData.lastDateToSubmitForm),
        createdById: user.zonalId,
      }

      if (program) {
        await api.put(`/programs/${program.id}`, payload)
      } else {
        await api.post('/programs', payload)
      }

      onSuccess()
    } catch (error) {
      alert(error.response?.data?.message || 'Failed to save program')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
      <TextField
        fullWidth
        label="Title"
        required
        value={formData.title}
        onChange={(e) => handleChange('title', e.target.value)}
        margin="normal"
      />
      <TextField
        fullWidth
        label="Description"
        multiline
        rows={3}
        value={formData.description}
        onChange={(e) => handleChange('description', e.target.value)}
        margin="normal"
      />
      <TextField
        fullWidth
        label="Location"
        required
        value={formData.location}
        onChange={(e) => handleChange('location', e.target.value)}
        margin="normal"
        helperText="If location is 'BEAS', it will be treated as BEAS location, otherwise NON_BEAS"
      />
      <TextField
        fullWidth
        select
        label="Status"
        required
        value={formData.status}
        onChange={(e) => handleChange('status', e.target.value)}
        margin="normal"
      >
        <MenuItem value="scheduled">Scheduled</MenuItem>
        <MenuItem value="active">Active</MenuItem>
        <MenuItem value="completed">Completed</MenuItem>
        <MenuItem value="cancelled">Cancelled</MenuItem>
      </TextField>
      <TextField
        fullWidth
        label="Max Sewadars"
        type="number"
        value={formData.maxSewadars}
        onChange={(e) => handleChange('maxSewadars', e.target.value)}
        margin="normal"
        inputProps={{ min: 1, step: 1 }}
        helperText="Positive numbers only (min: 1)"
      />

      <TextField
        fullWidth
        type="datetime-local"
        label="Last Date/Time to Apply"
        value={formData.lastDateToApply}
        onChange={(e) => handleChange('lastDateToApply', e.target.value)}
        margin="normal"
        InputLabelProps={{ shrink: true }}
        helperText="Optional. If set, applications will be blocked after this date/time."
      />

      <TextField
        fullWidth
        type="datetime-local"
        label="Last Date/Time to Submit Form"
        value={formData.lastDateToSubmitForm}
        onChange={(e) => handleChange('lastDateToSubmitForm', e.target.value)}
        margin="normal"
        InputLabelProps={{ shrink: true }}
        helperText="Optional. If set, form submissions will be blocked after this date/time."
      />

      <Box mt={2}>
        <Typography variant="subtitle2" gutterBottom>
          Program Dates *
        </Typography>
        {formData.programDates.map((dateObj, index) => (
          <Box key={index} display="flex" gap={1} mb={1}>
            <TextField
              type="date"
              required
              value={dateObj.date}
              onChange={(e) => handleDateChange(index, e.target.value)}
              sx={{ flex: 1 }}
              InputLabelProps={{ shrink: true }}
            />
            {formData.programDates.length > 1 && (
              <IconButton onClick={() => removeDate(index)} color="error">
                <DeleteIcon />
              </IconButton>
            )}
          </Box>
        ))}
        <Button
          startIcon={<AddIcon />}
          onClick={addDate}
          size="small"
          sx={{ mt: 1 }}
        >
          Add Date
        </Button>
      </Box>

      <Box display="flex" gap={2} justifyContent="flex-end" mt={3}>
        <Button onClick={onClose} disabled={loading}>
          Cancel
        </Button>
        <Button type="submit" variant="contained" disabled={loading}>
          {loading ? 'Saving...' : 'Save'}
        </Button>
      </Box>
    </Box>
  )
}

export default ProgramForm

