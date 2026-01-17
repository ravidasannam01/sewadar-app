import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import PrivateRoute from './components/PrivateRoute'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Programs from './pages/Programs'
import Applications from './pages/Applications'
import Admin from './pages/Admin'
import Workflow from './pages/Workflow'
import PendingActions from './pages/PendingActions'
import Attendance from './pages/Attendance'
import Layout from './components/Layout'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/"
          element={
            <PrivateRoute>
              <Layout />
            </PrivateRoute>
          }
        >
          <Route index element={<Navigate to="/programs" replace />} />
          <Route path="programs" element={<Programs />} />
          <Route path="applications" element={<Applications />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="admin" element={<Admin />} />
          <Route path="workflow" element={<Workflow />} />
          <Route path="attendance" element={<Attendance />} />
          <Route path="pending-actions" element={<PendingActions />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}

export default App

