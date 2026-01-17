import { createContext, useContext, useState, useEffect } from 'react'
import api from '../services/api'

const AuthContext = createContext(null)

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('authToken')
    const userData = localStorage.getItem('currentUser')
    
    if (token && userData) {
      try {
        const parsedUser = JSON.parse(userData)
        api.defaults.headers.common['Authorization'] = `Bearer ${token}`
        setUser(parsedUser)
      } catch (error) {
        console.error('Error parsing user data:', error)
        localStorage.removeItem('authToken')
        localStorage.removeItem('currentUser')
      }
    }
    setLoading(false)
  }, [])

  const login = async (zonalId, password) => {
    try {
      const response = await api.post('/auth/login', { zonalId, password })
      const { token, sewadar } = response.data
      
      if (!token || !sewadar) {
        return {
          success: false,
          error: 'Invalid response from server',
        }
      }
      
      localStorage.setItem('authToken', token)
      localStorage.setItem('currentUser', JSON.stringify(sewadar))
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`
      setUser(sewadar)
      return { success: true }
    } catch (error) {
      console.error('Login error:', error)
      const errorMessage = error.response?.data?.message || 
                          error.response?.data?.error || 
                          error.message || 
                          'Login failed. Please check your credentials.'
      return {
        success: false,
        error: errorMessage,
      }
    }
  }

  const logout = () => {
    localStorage.removeItem('authToken')
    localStorage.removeItem('currentUser')
    delete api.defaults.headers.common['Authorization']
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  )
}

