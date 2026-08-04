import { z } from 'zod'

export const verifyEmailSchema = z.object({
    email: z
        .string()
        .trim()
        .min(1, 'Email is required')
        .email('Email must be valid'),
    code: z.string().regex(/^\d{6}$/, 'Enter all 6 digits.'),
})