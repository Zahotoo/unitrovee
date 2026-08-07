import { z } from 'zod'

export const loginSchema = z.object({
    email: z
        .string()
        .trim()
        .min(1, 'Email is required')
        .email('Email must be valid')
        .max(254, 'Email must be at most 254 characters'),

    password: z
        .string()
        .min(1, 'Password is required')
        .min(8, 'Password must be between 8 and 72 characters')
        .max(72, 'Password must be between 8 and 72 characters'),
})

export type LoginFormValues = z.infer<typeof loginSchema>