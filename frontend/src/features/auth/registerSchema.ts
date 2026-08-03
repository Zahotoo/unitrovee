import { z } from "zod"

export const registerSchema = z.object({
    displayName: z
        .string()
        .trim()
        .min(1, 'Display name is required')
        .max(100, 'Display name must be at most 100 characters'),

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

export type RegisterFormValues = z.infer<typeof registerSchema>