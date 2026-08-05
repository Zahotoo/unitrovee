import { apiClient } from '@/api/client'

type ApiResponse<T> = {
    data: T
    message: string
}

export type RegisterCredentials = {
    displayName: string
    email: string
    password: string
}

export type SchoolResponse = {
    id: number
    name: string
    shortName: string
    emailDomain: string
    city: string
}

export type RegisterResponse = {
    id: number
    email: string
    displayName: string
    emailVerified: boolean
    school: SchoolResponse
}

export type VerifyEmailCredentials = {
    email: string
    code: string
}

export type VerifyEmailResponse = {
    email: string
    emailVerified: boolean
}

export type ResendVerificationCredentials = {
    email: string
}

export type ResendVerificationResponse = {
    retryAfterSeconds: number
}

export function registerAccount(
    credentials: RegisterCredentials,
): Promise<ApiResponse<RegisterResponse>> {
    return apiClient<ApiResponse<RegisterResponse>>('/auth/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(credentials),
    })
}

export function verifyEmail(
    credentials: VerifyEmailCredentials,
): Promise<ApiResponse<VerifyEmailResponse>> {
    return apiClient<ApiResponse<VerifyEmailResponse>>('/auth/verify', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(credentials),
    })
}

export function resendVerificationCode(
    credentials: ResendVerificationCredentials,
): Promise<ApiResponse<ResendVerificationResponse>> {
    return apiClient<ApiResponse<ResendVerificationResponse>>(
        '/auth/resend-verification',
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(credentials),
        },
    )
}