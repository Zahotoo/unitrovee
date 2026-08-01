import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'

import { QueryProvider } from '@/app/QueryProvider'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { router } from '@/routes/router'
import './styles/index.css'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <QueryProvider>
            <AuthProvider>
                <RouterProvider router={router} />
            </AuthProvider>
        </QueryProvider>
    </StrictMode>,
)
