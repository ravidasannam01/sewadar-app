import { useState, useEffect } from 'react'
import {
  TextField,
  Button,
  Box,
  IconButton,
  Typography,
  Grid,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
} from '@mui/material'
import { Add as AddIcon, Delete as DeleteIcon } from '@mui/icons-material'
import api from '../services/api'

const SewadarForm = ({ sewadar, onClose, onSuccess }) => {
  const [formData, setFormData] = useState({
    zonalId: '',
    firstName: '',
    lastName: '',
    mobile: '',
    password: '',
    location: '',
    profession: '',
    dateOfBirth: '',
    joiningDate: '',
    emergencyContact: '',
    emergencyContactRelationship: '',
    photoUrl: '',
    aadharNumber: '',
    languages: [''],
    address1: '',
    address2: '',
    email: '',
    remarks: '',
    fatherHusbandName: '',
    gender: '',
    screenerCode: '',
    satsangPlace: '',
    emailId: '',
  })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (sewadar) {
      setFormData({
        zonalId: sewadar.zonalId || '',
        firstName: sewadar.firstName || '',
        lastName: sewadar.lastName || '',
        mobile: sewadar.mobile || '',
        password: '',
        location: sewadar.location || '',
        profession: sewadar.profession || '',
        dateOfBirth: sewadar.dateOfBirth || '',
        joiningDate: sewadar.joiningDate || '',
        emergencyContact: sewadar.emergencyContact || '',
        emergencyContactRelationship: sewadar.emergencyContactRelationship || '',
        photoUrl: sewadar.photoUrl || '',
        aadharNumber: sewadar.aadharNumber || '',
        languages: sewadar.languages?.length > 0 ? sewadar.languages : [''],
        address1: sewadar.address?.address1 || '',
        address2: sewadar.address?.address2 || '',
        email: sewadar.address?.email || '',
        remarks: sewadar.remarks || '',
        fatherHusbandName: sewadar.fatherHusbandName || '',
        gender: sewadar.gender || '',
        screenerCode: sewadar.screenerCode || '',
        satsangPlace: sewadar.satsangPlace || '',
        emailId: sewadar.emailId || '',
      })
    }
  }, [sewadar])

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
  }

  const handleLanguageChange = (index, value) => {
    const newLanguages = [...formData.languages]
    newLanguages[index] = value
    setFormData((prev) => ({ ...prev, languages: newLanguages }))
  }

  const addLanguage = () => {
    setFormData((prev) => ({
      ...prev,
      languages: [...prev.languages, ''],
    }))
  }

  const removeLanguage = (index) => {
    const newLanguages = formData.languages.filter((_, i) => i !== index)
    setFormData((prev) => ({ ...prev, languages: newLanguages }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)

    const languages = formData.languages
      .map((lang) => lang.trim())
      .filter((lang) => lang.length > 0)
      .flatMap((lang) => lang.split(',').map((l) => l.trim()).filter((l) => l))

    try {
      const payload = {
        zonalId: formData.zonalId, // Include zonalId (required)
        firstName: formData.firstName,
        lastName: formData.lastName,
        mobile: formData.mobile,
        location: formData.location,
        profession: formData.profession,
        dateOfBirth: formData.dateOfBirth || null,
        joiningDate: formData.joiningDate || null,
        emergencyContact: formData.emergencyContact || null,
        emergencyContactRelationship: formData.emergencyContactRelationship || null,
        photoUrl: formData.photoUrl || null,
        aadharNumber: formData.aadharNumber || null,
        languages: languages.length > 0 ? languages : null,
        remarks: formData.remarks,
        address1: formData.address1,
        address2: formData.address2,
        email: formData.email,
        fatherHusbandName: formData.fatherHusbandName || null,
        gender: formData.gender || null,
        screenerCode: formData.screenerCode || null,
        satsangPlace: formData.satsangPlace || null,
        emailId: formData.emailId || null,
      }

      if (formData.password) {
        payload.password = formData.password
      }

      if (sewadar) {
        await api.put(`/sewadars/${sewadar.zonalId}`, payload)
      } else {
        await api.post('/sewadars', payload)
      }

      onSuccess()
    } catch (error) {
      alert(error.response?.data?.message || 'Failed to save sewadar')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="First Name"
            required
            value={formData.firstName}
            onChange={(e) => handleChange('firstName', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Last Name"
            required
            value={formData.lastName}
            onChange={(e) => handleChange('lastName', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Zonal ID"
            required={!sewadar}
            value={formData.zonalId}
            onChange={(e) => handleChange('zonalId', e.target.value)}
            disabled={!!sewadar} // Cannot change zonalId after creation
            helperText={sewadar ? 'Zonal ID cannot be changed' : 'Unique organizational identity (required)'}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Mobile Number"
            required
            value={formData.mobile}
            onChange={(e) => handleChange('mobile', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Password"
            type="password"
            required={!sewadar}
            value={formData.password}
            onChange={(e) => handleChange('password', e.target.value)}
            helperText={sewadar ? 'Leave blank to keep current password' : ''}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Location/Center"
            value={formData.location}
            onChange={(e) => handleChange('location', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Profession"
            value={formData.profession}
            onChange={(e) => handleChange('profession', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            type="date"
            label="Date of Birth"
            value={formData.dateOfBirth}
            onChange={(e) => handleChange('dateOfBirth', e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            type="date"
            label="Joining Date"
            value={formData.joiningDate}
            onChange={(e) => handleChange('joiningDate', e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Emergency Contact"
            value={formData.emergencyContact}
            onChange={(e) => handleChange('emergencyContact', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Emergency Contact Relationship"
            value={formData.emergencyContactRelationship}
            onChange={(e) => handleChange('emergencyContactRelationship', e.target.value)}
            placeholder="e.g., Father, Mother, Spouse"
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Photo URL"
            type="url"
            value={formData.photoUrl}
            onChange={(e) => handleChange('photoUrl', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Aadhar Number"
            value={formData.aadharNumber}
            onChange={(e) => {
              const value = e.target.value.replace(/[^0-9]/g, '').slice(0, 12)
              handleChange('aadharNumber', value)
            }}
            inputProps={{ maxLength: 12 }}
            helperText="12-digit Aadhar number (numbers only)"
          />
        </Grid>
        <Grid item xs={12}>
          <Typography variant="subtitle2" gutterBottom>
            Languages Known
          </Typography>
          {formData.languages.map((lang, index) => (
            <Box key={index} display="flex" gap={1} mb={1}>
              <TextField
                fullWidth
                value={lang}
                onChange={(e) => handleLanguageChange(index, e.target.value)}
                placeholder="e.g., Hindi, English"
                size="small"
              />
              {formData.languages.length > 1 && (
                <IconButton onClick={() => removeLanguage(index)} color="error" size="small">
                  <DeleteIcon />
                </IconButton>
              )}
            </Box>
          ))}
          <Button
            startIcon={<AddIcon />}
            onClick={addLanguage}
            size="small"
            sx={{ mt: 1 }}
          >
            Add Language
          </Button>
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            label="Address Line 1"
            value={formData.address1}
            onChange={(e) => handleChange('address1', e.target.value)}
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            label="Address Line 2"
            value={formData.address2}
            onChange={(e) => handleChange('address2', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Father/Husband Name"
            value={formData.fatherHusbandName}
            onChange={(e) => handleChange('fatherHusbandName', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <FormControl fullWidth>
            <InputLabel>Gender</InputLabel>
            <Select
              value={formData.gender}
              onChange={(e) => handleChange('gender', e.target.value)}
              label="Gender"
            >
              <MenuItem value="">None</MenuItem>
              <MenuItem value="MALE">Male</MenuItem>
              <MenuItem value="FEMALE">Female</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Screener Code"
            value={formData.screenerCode}
            onChange={(e) => handleChange('screenerCode', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Satsang Place"
            value={formData.satsangPlace}
            onChange={(e) => handleChange('satsangPlace', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Email ID"
            type="email"
            value={formData.emailId}
            onChange={(e) => handleChange('emailId', e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Email (Address)"
            type="email"
            value={formData.email}
            onChange={(e) => handleChange('email', e.target.value)}
            helperText="Email in address (legacy field)"
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            label="Remarks"
            multiline
            rows={3}
            value={formData.remarks}
            onChange={(e) => handleChange('remarks', e.target.value)}
          />
        </Grid>
      </Grid>

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

export default SewadarForm

